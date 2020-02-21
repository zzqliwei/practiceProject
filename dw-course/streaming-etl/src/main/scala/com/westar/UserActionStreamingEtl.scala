package com.westar

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import org.apache.spark.SparkConf
import org.apache.spark.sql.{SaveMode, SparkSession}
import org.apache.spark.streaming.flume.FlumeUtils
import org.apache.spark.streaming.{Milliseconds, StreamingContext}

object UserActionStreamingEtl {
  def main(args: Array[String]): Unit = {
    val Array(host,port) = Array("master","44446")
    //时间间隔
    val batchInterval = Milliseconds(1000)
    //创建一个环境和批处理大小
    val sparkConf = new SparkConf()
      .setAppName("UserActionStreamingEtl")
      .setMaster("local[2]")
    val outputPath = sparkConf.get("spark.user.action.outputPath",
      "hdfs://master:8020/user/hadoop/dw-course/streaming-etl/user-action-parquet")
    val ssc = new StreamingContext(sparkConf,batchInterval)

    // Create a flume stream that polls the Spark Sink running in a Flume agent
    val flumeEventStream = FlumeUtils.createPollingStream(ssc,host,port.toInt)
    val userActionStream = flumeEventStream.map(sparkFlumeEvent =>{
      val avroFlumeEvent = sparkFlumeEvent.event
      val headers: java.util.Map[CharSequence, CharSequence]  = avroFlumeEvent.getHeaders

      val hostname:String = headers.get("host").toString
      val body:ByteBuffer = avroFlumeEvent.getBody
      val line = StandardCharsets.UTF_8.decode(body).toString

      val fields = line.split("\t");
      //解析出系统平台的信息以及浏览器的信息(浏览器的类型以及版本)
      val userAgent = UserAgentUtil.getUserAgent(fields(5))
      val actionTime = fields(0)
      val (year, month, day) = getYearMonthDay(actionTime)
      UserAction(hostname, fields(2), actionTime, fields(1), fields(3), fields(4), fields(5), fields(6), fields(7).toInt,
        fields(8).toInt, userAgent.getBrowserType, userAgent.getBrowserVersion,
        userAgent.getPlatformType, userAgent.getPlatformSeries, userAgent.getPlatformVersion, year, month, day)

    })

    userActionStream.foreachRDD(userActionRdd =>{
      val spark = SparkSession.builder().config(userActionRdd.sparkContext.getConf)
        .getOrCreate()
      import spark.implicits._
      val userActionDS = spark.createDataset(userActionRdd)
      userActionDS.write.mode(SaveMode.Append)
        .partitionBy("year","month","day")
        .parquet(outputPath)
    })

    ssc.start()
    ssc.awaitTermination()



  }
  //取得年月日
  def getYearMonthDay(actionTime:String):(Int,Int,Int) ={
    val temp = actionTime.replaceAll("-", "")
    (temp.substring(0, 4).toInt, temp.substring(0, 6).toInt, temp.substring(0, 8).toInt)
  }

}

case class UserAction(hostname: String, userId: String, actionTime: String, clientIp: String, browserName: String,
                      browserCode: String, browserUserAgent: String, currentLang: String,
                      screenWidth: Int, screenHeight: Int, browserType: String, browserVersion: String,
                      platformType: String, platformSeries: String, platformVersion: String, year: Int, month: Int, day: Int)
