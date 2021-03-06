package main.scala

import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types._
import org.apache.spark.sql.catalyst.ScalaReflection
import org.apache.spark.sql.{Dataset, Row}
import org.tpch.tablereader._

// TPC-H table schemas
case class Customer(
  c_custkey: Long,
  c_name: String,
  c_address: String,
  c_nationkey: Long,
  c_phone: String,
  c_acctbal: Double,
  c_mktsegment: String,
  c_comment: String)

case class Lineitem(
  l_orderkey: Long,
  l_partkey: Long,
  l_suppkey: Long,
  l_linenumber: Long,
  l_quantity: Double,
  l_extendedprice: Double,
  l_discount: Double,
  l_tax: Double,
  l_returnflag: String,
  l_linestatus: String,
  l_shipdate: String,
  l_commitdate: String,
  l_receiptdate: String,
  l_shipinstruct: String,
  l_shipmode: String,
  l_comment: String)

case class Nation(
  n_nationkey: Long,
  n_name: String,
  n_regionkey: Long,
  n_comment: String)

case class Order(
  o_orderkey: Long,
  o_custkey: Long,
  o_orderstatus: String,
  o_totalprice: Double,
  o_orderdate: String,
  o_orderpriority: String,
  o_clerk: String,
  o_shippriority: Long,
  o_comment: String)

case class Part(
  p_partkey: Long,
  p_name: String,
  p_mfgr: String,
  p_brand: String,
  p_type: String,
  p_size: Long,
  p_container: String,
  p_retailprice: Double,
  p_comment: String)

case class Partsupp(
  ps_partkey: Long,
  ps_suppkey: Long,
  ps_availqty: Long,
  ps_supplycost: Double,
  ps_comment: String)

case class Region(
  r_regionkey: Long,
  r_name: String,
  r_comment: String)

case class Supplier(
  s_suppkey: Long,
  s_name: String,
  s_address: String,
  s_nationkey: Long,
  s_phone: String,
  s_acctbal: Double,
  s_comment: String)

sealed trait FileType
case object CSVS3 extends FileType
case object CSVFile extends FileType
case object TBLFile extends FileType
case object TBLS3 extends FileType
class TpchSchemaProvider(sc: SparkContext, 
                         inputDir: String, 
                         s3Select: TpchS3Options,
                         fileType: FileType,
                         partitions: Int) {

  // this is used to implicitly convert an RDD to a DataFrame.
  val sqlContext = new org.apache.spark.sql.SQLContext(sc)
  import sqlContext.implicits._
  val dfMap = 
    if (fileType == CSVS3) 
      Map(
          "customer" -> TpchTableReaderS3.readTable[Customer]("customer.csv", inputDir, s3Select, partitions),
          "lineitem" -> TpchTableReaderS3.readTable[Lineitem]("lineitem.csv", inputDir, s3Select, partitions),
          "nation" -> TpchTableReaderS3.readTable[Nation]("nation.csv", inputDir, s3Select, partitions),
          "region" -> TpchTableReaderS3.readTable[Region]("region.csv", inputDir, s3Select, partitions),
          "order" -> TpchTableReaderS3.readTable[Order]("order.csv", inputDir, s3Select, partitions),
          "part" -> TpchTableReaderS3.readTable[Part]("part.csv", inputDir, s3Select, partitions),
          "partsupp" -> TpchTableReaderS3.readTable[Partsupp]("partsupp.csv", inputDir, s3Select, partitions),
          "supplier" -> TpchTableReaderS3.readTable[Supplier]("supplier.csv", inputDir, s3Select, partitions) )
    else if (fileType == CSVFile) 
      Map(
          "customer" -> TpchTableReaderFile.readTable[Customer]("customer", inputDir),
          "lineitem" -> TpchTableReaderFile.readTable[Lineitem]("lineitem", inputDir),
          "nation" -> TpchTableReaderFile.readTable[Nation]("nation", inputDir),
          "region" -> TpchTableReaderFile.readTable[Region]("region", inputDir),
          "order" -> TpchTableReaderFile.readTable[Order]("order", inputDir),
          "part" -> TpchTableReaderFile.readTable[Part]("part", inputDir),
          "partsupp" -> TpchTableReaderFile.readTable[Partsupp]("partsupp", inputDir),
          "supplier" -> TpchTableReaderFile.readTable[Supplier]("supplier", inputDir) )
    else if (fileType == TBLS3)
      Map(
          "customer" -> TpchTableReaderS3.readTable[Customer]("customer.tbl", inputDir, s3Select, partitions),
          "lineitem" -> TpchTableReaderS3.readTable[Lineitem]("lineitem.tbl", inputDir, s3Select, partitions),
          "nation" -> TpchTableReaderS3.readTable[Nation]("nation.tbl", inputDir, s3Select, partitions),
          "region" -> TpchTableReaderS3.readTable[Region]("region.tbl", inputDir, s3Select, partitions),
          "order" -> TpchTableReaderS3.readTable[Order]("order.tbl", inputDir, s3Select, partitions),
          "part" -> TpchTableReaderS3.readTable[Part]("part.tbl", inputDir, s3Select, partitions),
          "partsupp" -> TpchTableReaderS3.readTable[Partsupp]("partsupp.tbl", inputDir, s3Select, partitions),
          "supplier" -> TpchTableReaderS3.readTable[Supplier]("supplier.tbl", inputDir, s3Select, partitions) )
    else
      Map(
        "customer" -> sc.textFile(inputDir + "/customer.tbl*").map(_.split('|')).map(p =>
          Customer(p(0).trim.toLong, p(1).trim, p(2).trim, p(3).trim.toLong, p(4).trim, p(5).trim.toDouble, p(6).trim, p(7).trim)).toDF(),
        "lineitem" -> sc.textFile(inputDir + "/lineitem.tbl*").map(_.split('|')).map(p =>
          Lineitem(p(0).trim.toLong, p(1).trim.toLong, p(2).trim.toLong, p(3).trim.toLong, p(4).trim.toDouble, p(5).trim.toDouble, p(6).trim.toDouble, p(7).trim.toDouble, p(8).trim, p(9).trim, p(10).trim, p(11).trim, p(12).trim, p(13).trim, p(14).trim, p(15).trim)).toDF(),
        "nation" -> sc.textFile(inputDir + "/nation.tbl*").map(_.split('|')).map(p =>
          Nation(p(0).trim.toLong, p(1).trim, p(2).trim.toLong, p(3).trim)).toDF(),
        "region" -> sc.textFile(inputDir + "/region.tbl*").map(_.split('|')).map(p =>
          Region(p(0).trim.toLong, p(1).trim, p(2).trim)).toDF(),
        "order" -> sc.textFile(inputDir + "/orders.tbl*").map(_.split('|')).map(p =>
          Order(p(0).trim.toLong, p(1).trim.toLong, p(2).trim, p(3).trim.toDouble, p(4).trim, p(5).trim, p(6).trim, p(7).trim.toLong, p(8).trim)).toDF(),
        "part" -> sc.textFile(inputDir + "/part.tbl*").map(_.split('|')).map(p =>
          Part(p(0).trim.toLong, p(1).trim, p(2).trim, p(3).trim, p(4).trim, p(5).trim.toLong, p(6).trim, p(7).trim.toDouble, p(8).trim)).toDF(),
        "partsupp" -> sc.textFile(inputDir + "/partsupp.tbl*").map(_.split('|')).map(p =>
          Partsupp(p(0).trim.toLong, p(1).trim.toLong, p(2).trim.toLong, p(3).trim.toDouble, p(4).trim)).toDF(),
        "supplier" -> sc.textFile(inputDir + "/supplier.tbl*").map(_.split('|')).map(p =>
          Supplier(p(0).trim.toLong, p(1).trim, p(2).trim, p(3).trim.toLong, p(4).trim, p(5).trim.toDouble, p(6).trim)).toDF())

  // for implicits
  val customer = dfMap.get("customer").get
  val lineitem = dfMap.get("lineitem").get
  val nation = dfMap.get("nation").get
  val region = dfMap.get("region").get
  val order = dfMap.get("order").get
  val part = dfMap.get("part").get
  val partsupp = dfMap.get("partsupp").get
  val supplier = dfMap.get("supplier").get

  dfMap.foreach {
    case (key, value) => value.createOrReplaceTempView(key)
  }
}
