package org.tpch.tablereader

import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types._
import org.apache.spark.sql.catalyst.ScalaReflection
import org.apache.spark.sql.{Dataset, Row}
import scala.reflect.runtime.universe._

case class TpchS3Options(enableFilter: Boolean,
                         enableProject: Boolean,
                         enableAggregate: Boolean,
                         explain: Boolean) {

    def isEnabled() : Boolean = {
      enableFilter && enableProject && enableAggregate
    }
}

object TpchTableReaderS3 {
  
  private val s3IpAddr = "minioserver"
  private val sparkSession = SparkSession.builder
      .master("local[2]")
      .appName("TpchProvider")
      .config("spark.datasource.s3.endpoint", s"""http://$s3IpAddr:9000""")
      .config("spark.datasource.s3.accessKey", "admin")
      .config("spark.datasource.s3.secretKey", "admin123")
      .getOrCreate()

  def readTable[T: WeakTypeTag]
               (name: String, inputDir: String,
                s3Options: TpchS3Options, partitions: Int)
               (implicit tag: TypeTag[T]): Dataset[Row] = {
    val schema = ScalaReflection.schemaFor[T].dataType.asInstanceOf[StructType]
    if (s3Options.isEnabled()) {
      val df = sparkSession.read
        .format("com.github.s3datasource") // "org.apache.spark.sql.execution.datasources.v2.s3"
        .option("format", "csv")
        .option("partitions", partitions)
        .schema(schema)
        .load(inputDir + "/" +  name)
        df
    } else {
      val df = sparkSession.read
        .format("com.github.s3datasource")
        .option("format", "csv")
        .option("partitions", partitions)
        .option((if (s3Options.enableFilter) "Enable" else "Disable") + "FilterPush", "")
        .option((if (s3Options.enableProject) "Enable" else "Disable") + "ProjectPush", "")
        .option((if (s3Options.enableAggregate) "Enable" else "Disable") + "AggregatePush", "")
        .schema(schema)
        .load(inputDir + "/" +  name)
        df
    }
  }
}
object TpchTableReaderFile {
  
  private val sparkSession = SparkSession.builder
      .master("local[2]")
      .appName("TpchProvider")
      .getOrCreate()

  def readTable[T: WeakTypeTag]
               (name: String, inputDir: String)
               (implicit tag: TypeTag[T]): Dataset[Row] = {
    val schema = ScalaReflection.schemaFor[T].dataType.asInstanceOf[StructType]
    val df = sparkSession.read
        .format("csv")
        .schema(schema)
        .load(inputDir + "/" +  name + ".csv")
        // df.show()
        df
  }
}
