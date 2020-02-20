package com.westar.spark.etl

import java.util.Properties

import org.apache.spark.sql.SparkSession

/**
 * 将hive数据保存到mysql
 */
object AvgMovieRatingExporter {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder()
      .master("local")
      .appName("AvgMovieRatingExporter")
      .enableHiveSupport()
      .getOrCreate()

    val url = "jdbc:mysql://master:3306/movie"
    val table = "avg_movie_rating"

    val connectionProperties = new Properties()
    connectionProperties.put("user", "root")
    connectionProperties.put("password", "WESTAR@soft1")

    spark.read.table("movielens.avg_movie_rating")
      .write
      .jdbc(url,table,connectionProperties)

  }

}
