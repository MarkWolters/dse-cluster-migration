package phact

import com.datastax.spark.connector.cql.{CassandraConnector, TableDef}
import org.apache.spark.sql.SparkSession
import com.datastax.spark.connector._
import org.apache.spark.SparkContext

import scala.io.Source

object MigratePayments extends App {

  val spark = SparkSession.builder
    .appName("dse-payment-migration")
    .enableHiveSupport()
    .getOrCreate()

  var conf = spark.sparkContext.getConf

  var clusterHostOne        = conf.get("spark.dse.cluster.migration.fromClusterHost", null)
  var clusterHostTwo        = conf.get("spark.dse.cluster.migration.toClusterHost", null)
  var fromKeyspace          = conf.get("spark.dse.cluster.migration.fromKeyspace", "payments")
  var toKeyspace            = conf.get("spark.dse.cluster.migration.toKeyspace", "payments")
  var fromuser              = conf.get("spark.dse.cluster.migration.fromuser", null)
  var frompassword          = conf.get("spark.dse.cluster.migration.frompassword", null)
  var touser                = conf.get("spark.dse.cluster.migration.touser", null)
  var topassword            = conf.get("spark.dse.cluster.migration.topassword", null)
  var idFile                = conf.get("spark.dse.cluster.migration.idFile", "paymentIDs.csv")
  var tableName             = conf.get("spark.dse.cluster.migration.table", "payment_transactions_detail_dpt")
  var partitionKey          = conf.get("spark.dse.cluster.migration.partitionKey", "dps_payment_id")

  var connectorToClusterOne : CassandraConnector = _
  var connectorToClusterTwo : CassandraConnector  = _

  if (fromuser != null && frompassword != null) {
    connectorToClusterOne = CassandraConnector(spark.sparkContext.getConf.set("spark.cassandra.connection.host", clusterHostOne).set("spark.cassandra.auth.username", fromuser).set("spark.cassandra.auth.password", frompassword))
  }else{
    connectorToClusterOne = CassandraConnector(spark.sparkContext.getConf.set("spark.cassandra.connection.host", clusterHostOne))
  }
  if (touser != null && topassword != null) {
    connectorToClusterTwo = CassandraConnector(spark.sparkContext.getConf.set("spark.cassandra.connection.host", clusterHostTwo).set("spark.cassandra.auth.username", touser).set("spark.cassandra.auth.password", topassword))
  }else{
    connectorToClusterTwo = CassandraConnector(spark.sparkContext.getConf.set("spark.cassandra.connection.host", clusterHostTwo))
  }

  val file = Source.fromFile(idFile)

  for (id <- file.getLines) {
    println(s"\tMigrating ${partitionKey} $id")
    try {
      val paymentRdd = {
        implicit val c = connectorToClusterOne
        spark.sparkContext.cassandraTable(fromKeyspace, tableName).where(s"${partitionKey} = ?", id);
      }

      {
        implicit val c = connectorToClusterTwo
        val clusterTwoRdd = spark.sparkContext.cassandraTable(toKeyspace, tableName).where(s"${partitionKey} = ?", id);
        if (clusterTwoRdd.isEmpty()) {
          paymentRdd.saveToCassandra(toKeyspace, tableName)
          println(s"${partitionKey} $id written")
        }
        else {
          println(s"${partitionKey} $id already exists")
        }
      }

    } catch {
      case e: Exception =>
        println(s"\tException migrating data for ${partitionKey} $id: ${e.getMessage}")
        e.printStackTrace()
    }
  }

  spark.stop()
  sys.exit(0)
}
