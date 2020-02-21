package com.westar

import org.apache.spark.sql.{SaveMode, SparkSession}

/**
 * spark-subimit --class com.westar.SmallFileMerger \
 * --master spark://master:7077 \
 * --deploy-mode client \
 * --driver-memory 512m \
 * --executor-memory 512m \
 * --total-executor-cores 2 \
 * --num-executors 2 \
 * --executor-cores 1 \
 * --conf spark.small.file.merge.inputPath=hdfs://master:8020/user/hadoop/dw-course/streaming-etl/user-action-parquet/year=2018/month=201806/day=20180613
 * --conf spark.small.file.merge.outputPath=hdfs://master:8020/user/hadoop/dw-course/streaming-etl/user-action-merged/year=2018/month=201806/day=20180613
 * --conf spark.small.file.merge.numberPartition=2
 * /home/hadoop/dw-course/streaming-etl_1.0-SNAPSHOT.jar
 */
object SmallFileMerger {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .master("local[*]")
      .appName("SmallFileMerger")
      .getOrCreate()
    val inputpath = spark.conf.get("spark.small.file.merge.inputPath",
      "hdfs://master:8020/user/hadoop/dw-course/streaming-etl/user-action-parquet/year=2018/month=201806/day=20180613")

    val numberPartition = spark.conf.get("spark.small.file.merge.numberPartition", "2").toInt

    val outputPath = spark.conf.get("spark.small.file.merge.outputPath",
      "hdfs://master:8020/user/hadoop/dw-course/streaming-etl/user-action-merged/year=2018/month=201806/day=20180613")

    spark.read.parquet(inputpath)
      .repartition(numberPartition)
      .write
      .mode(SaveMode.Overwrite)
      .parquet(outputPath)

    spark.stop()
  }

}
