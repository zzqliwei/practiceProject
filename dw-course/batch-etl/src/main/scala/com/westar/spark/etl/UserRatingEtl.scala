package com.westar.spark.etl

import java.util.Properties

import org.apache.spark.sql.SparkSession

/**
 * spark-submit --class com.westar.spark.etl.UserRatingEtl \
 * --master spark:/master:7077 \
 * --deploy-mode client \
 * --driver-memory 512m \
 * --executor-memory 512m \
 * --total-exeutor-cores 2 \
 * --executors-cores 1 \
 * --jar /home/hadoop/dw-course/mysql-connector-java-5.1.44-bin.jar \
 * /home/hadoop/dw-course/batch-etl-1.0-SNAPSHOT.jar
 *
 * 实现将mysql数据库的信息，保存到hive中 也可以执行sqoop
 */
object UserRatingEtl {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder()
      .master("local")
      .appName("UserRatingEtl")
      .enableHiveSupport()
      .getOrCreate()


    val url = "jdbc:mysql://master:3306/movie"
    val table = "user_rating"
    //两个条件的数据
    val predicates = Array("dt_time between '1970-01-11 00:00:00' and '1970-01-11 12:00:00'",
      "dt_time between '1970-01-11 12:00:01' and '1970-01-11 23:59:59'")

    val connectionProperties = new Properties()
    connectionProperties.put("user", "root")
    connectionProperties.put("password", "WESTAR@soft1")

    spark.read.jdbc(url,table,predicates,connectionProperties)
      .write
      .saveAsTable("movielens.user_rating_spark")
    spark.stop()
  }

}
