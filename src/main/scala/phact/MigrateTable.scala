package phact

import com.datastax.spark.connector._
import com.datastax.spark.connector.cql.{CassandraConnector, TableDef}
import org.apache.spark.SparkContext

object MigrateTable {

  def migrateTable(sc: SparkContext,
                   connectorToClusterOne: CassandraConnector,
                   connectorToClusterTwo: CassandraConnector,
                   deltaTime: String = null,
                   timeColumn: String = null,
                   fromKeyspace: String,
                   fromTable: String,
                   toKeyspace: String,
                   toTable: String,
                   newTableFlag: Boolean = false) {

    println(s"Reading from $fromKeyspace.$fromTable")

    val rddFromClusterOne = {
      // Sets connectorToClusterOne as default connection for everything in this code block
      implicit val c = connectorToClusterOne
      if (deltaTime == null && timeColumn == null) {
        sc.cassandraTable(fromKeyspace, fromTable)
      } else {
        sc.cassandraTable(fromKeyspace, fromTable).where(s"${timeColumn} > ?", deltaTime);
      }
    }

    println(s"Writing to $toKeyspace.$toTable")

    {
      //Sets connectorToClusterTwo as the default connection for everything in this code block
      implicit val c = connectorToClusterTwo
      if (newTableFlag) {
        var tableDef = TableDef(toKeyspace, toTable, rddFromClusterOne.tableDef.partitionKey, rddFromClusterOne.tableDef.clusteringColumns, rddFromClusterOne.tableDef.regularColumns, rddFromClusterOne.tableDef.indexes, false)
        rddFromClusterOne.saveAsCassandraTableEx(tableDef)
      } else {
        rddFromClusterOne.saveToCassandra(toKeyspace, toTable)
      }
    }
  }

}
