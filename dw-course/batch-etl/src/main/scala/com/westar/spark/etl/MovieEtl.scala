package com.westar.spark.etl

import java.util.Properties

import org.apache.spark.sql.SparkSession

/**
 * spark-submit --class com.westar.spark.etl.MovieEtl \
 * --master spark:/master:7077 \
 * --deploy-mode client \
 * --driver-memory 512m \
 * --executor-memory 512m \
 * --total-exeutor-cores 2 \
 * --executors-cores 1 \
 * --jar /home/hadoop/dw-course/mysql-connector-java-5.1.44-bin.jar \
 * /home/hadoop/dw-course/batch-etl-1.0-SNAPSHOT.jar
 *
 * 本地运行的条件：
 * 1、hadoop的配置文件core-site.xml和hdfs-site.xml以及hive的配置文件hive-site.xml必须放在工程的resources下
 * 2、spark应用需要设置local模式
 * 3、pom.xml中需要依赖mysql的jdbc的驱动jar包
 *
 *
 * 实现将mysql数据库的信息，保存到hive中 也可以执行sqoop
 */
object MovieEtl {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder()
      .master("local")
      .appName("MovieEtl")
      .enableHiveSupport()
      .getOrCreate()

    val url = "jdbc:mysql://master:3306/movie"
    val table = "movie"
    val columnName = "id"
    val lowerBound = 100
    val upperBound = 1000
    val numPartitions = 3

    val connectionProperties = new Properties()
    connectionProperties.put("user", "root")
    connectionProperties.put("password", "WESTAR@soft1")

    spark.read.jdbc(url,table,columnName,lowerBound,upperBound,numPartitions,connectionProperties)
      .write.saveAsTable("movielens.movie_spark")
    spark.stop()
  }

}
