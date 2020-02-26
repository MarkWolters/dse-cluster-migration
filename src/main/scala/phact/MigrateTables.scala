package phact

import com.datastax.spark.connector.cql.CassandraConnector
import org.apache.spark.sql.SparkSession

import scala.io.Source

object MigrateTables extends App {

  val spark = SparkSession.builder
    .appName("dse-cluster-migration")
    .enableHiveSupport()
    .getOrCreate()

  var conf = spark.sparkContext.getConf

  var clusterHostOne        = conf.get("spark.dse.cluster.migration.fromClusterHost", null)
  var clusterHostTwo        = conf.get("spark.dse.cluster.migration.toClusterHost", null)
  var fromKeyspace          = conf.get("spark.dse.cluster.migration.fromKeyspace", null)
  var toKeyspace            = conf.get("spark.dse.cluster.migration.toKeyspace", fromKeyspace)
  var newTableFlag          = conf.get("spark.dse.cluster.migration.newtableflag", "false").toBoolean
  var fromuser              = conf.get("spark.dse.cluster.migration.fromuser", null)
  var frompassword          = conf.get("spark.dse.cluster.migration.frompassword", null)
  var touser                = conf.get("spark.dse.cluster.migration.touser", null)
  var topassword            = conf.get("spark.dse.cluster.migration.topassword", null)
  var deltaTime             = conf.get("spark.dse.cluster.migration.deltaTime", null)
  var timeColumn            = conf.get("spark.dse.cluster.migration.timeColumn", null)
  var tableFile             = conf.get("spark.dse.cluster.migration.tableFile", "tables.csv")

  // Move these to file read
  //var fromTable             = conf.get("spark.dse.cluster.migration.fromTable", null)
  //var toTable               = conf.get("spark.dse.cluster.migration.toTable", fromTable)
  // End

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

  val file = Source.fromFile(tableFile).getLines.filter(f => !f.trim.isEmpty)
  val allTables = file.map(m2 => (m2.split("->")(0), m2.split("->")(1))).toMap

  for ((fromTable: String, toTable: String) <- allTables) {
    println(s"\tMigrating from $fromTable to $toTable")
    try {
      MigrateTable.migrateTable(spark.sparkContext,
        connectorToClusterOne,
        connectorToClusterTwo,
        deltaTime,
        timeColumn,
        fromKeyspace,
        fromTable,
        toKeyspace,
        toTable,
        newTableFlag
      )
      println(s"\tMigration from $fromTable to $toTable was successful")
    } catch {
      case e: Exception =>
        println(s"\tException when migrating data from $fromTable to $toTable: ${e.getMessage}")
        e.printStackTrace()
    }
  }

  spark.stop()
  sys.exit(0)
}
