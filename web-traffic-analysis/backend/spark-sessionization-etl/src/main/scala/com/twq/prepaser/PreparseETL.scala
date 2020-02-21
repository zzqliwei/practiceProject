package com.twq.prepaser

import com.westar.prepaser.{PreParsedLog, WebLogPreParser}
import org.apache.spark.SparkConf
import org.apache.spark.sql.{Encoders, SaveMode, SparkSession}

/**
 * export HADOOP_CONF_DIR=/home/hadoop/bigdata/hadoop-2.7.5/etc/hadoop/
*spark-submit --master yarn \
*--class com.westar.prepaser.PreparseETL \
*--driver-memory 512M \
*--executor-memory 512M \
*--num-executors 2 \
*--executor-cores 1 \
*--conf spark.traffic.analysis.rawdata.input=hdfs://master:9999/user/hadoop-twq/traffic-analysis/rawlog/20180617 \
* /home/hadoop-twq/traffice-analysis/jars/spark-preparse-etl-1.0-SNAPSHOT-jar-with-dependencies.jar prod
*/

object PreparseETL {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf()
    if(args.isEmpty){
      conf.setMaster("local")
    }
    val spark = SparkSession
      .builder()
      .appName("PreparseETL")
      .config(conf)
      .enableHiveSupport()
      .getOrCreate()

    val rawdataInputPath = spark.conf.get("spark.traffic.analysis.rawdata.input",
      "hdfs://master:8020/user/hadoop/traffic-analysis/rawlog/20180616")

    val numberPartitions = spark.conf.get("spark.traffic.analysis.rawdata.numberPartitions", "2").toInt

    val preParsedLogRDD = spark.sparkContext.textFile(rawdataInputPath)
      .flatMap(line => Option(WebLogPreParser.parse(line)))

    val preParsedLogDS = spark.createDataset(preParsedLogRDD)(Encoders.bean(classOf[PreParsedLog]))

    preParsedLogDS.coalesce(numberPartitions)
      .write
      .mode(SaveMode.Append)
      .partitionBy("year","month","day")
      .saveAsTable("rawdata.web")
  }
}
