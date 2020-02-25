package com.westar.sessionize.option

import java.text.SimpleDateFormat

import com.westar.sessionize.StreamingStopper
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.streaming.{Seconds, StreamingContext}

import scala.util.Properties

/**
  * 使用checkpoint机制来保存driver端的状态
  * 从而使得driver端容错：
  *  保证driver端突然间失败或者重启的时候数据不会丢失,但是使用这种方式有缺点就是：
  *   1）当程序升级的时候更新后的代码和更新前的代码不一样。导致程序运行报序列化错误
  *   2）checkpoint写HDFS文件需要花不少时间
  *
  *   采用zookeeper 读取数据大方式
  */
object StreamingContextCreator extends StreamingStopper{

  private val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

  def main(args: Array[String]): Unit = {
    def functionToCreateContext():StreamingContext = {

      val sparkConf  = new SparkConf()
        .setAppName("StreamingContextCreator")
        //清除不需要的数据
        .set("spark.cleaner.ttl","12000")

      sparkConf.setMaster("local[2]")
      val sc = new SparkContext(sparkConf)
      val ssc = new StreamingContext(sc,Seconds(10))

      //通过kafka方式接收数据
      val topicsSet = args(0).split(",").toSet
      val brokers = args(1)
      val kafkaParams = Map[String, Object](
        "bootstrap.servers" -> brokers,
        "group.id" -> "group",
        "key.deserializer" -> "org.apache.kafka.common.serialization.StringDeserializer",
        "value.deserializer" -> "org.apache.kafka.common.serialization.StringDeserializer",
        "auto.offset.reset" -> "latest",
        "enable.auto.commit" -> (false: java.lang.Boolean))

      // 1、LocationStrategies的取值：
      //    PreferBrokers 如果Kafka的broker和Spark Streaming的Executor在同一台机器上的话则选择这个
      //    PreferConsistent 一般都是使用这个，使用这种方式可以保证Kafka Topic的分区数据均匀分布在所有的Spark Streaming executors上
      //    PreferFixed 将指定的Topic的Partition的数据放到指定的机器上进行处理
      // 2、ConsumerStrategies的取值：
      //    Assign 指定消费特定的Topic的Partition数据
      //    Subscribe 订阅好几个topic的数据
      //    SubscribePattern 采用正则表达式匹配topic的名称，如果匹配上的话则订阅该topic的数据
      val line:DStream[String] = KafkaUtils.createDirectStream[Object,String](
        ssc,LocationStrategies.PreferConsistent,
        ConsumerStrategies.Subscribe[Object,String](
          topicsSet,kafkaParams))
        .map(_.value())

      //将原始事件日志解析转换成(ip, (startTime, endTime, record))
      val ipKeyLines = line.map[(String,(Long,Long,String))](eventRecord =>{
        //对event进行预解析
        val time = dateFormat.parse(eventRecord.substring(eventRecord.indexOf('[') + 1, eventRecord.indexOf(']'))).getTime()
        val ipAddress = eventRecord.substring(0,eventRecord.indexOf(" "))
        //返回了两个time
        //第一个time表示这个ip的起始访问时间，第二个time表示这个ip的最后访问时间
        (ipAddress, (time, time, eventRecord))
      })

      ipKeyLines.cache()
      println("ipKeyLines ------ ")
      ipKeyLines.print(200)

      ssc.checkpoint(args(2))
      ssc

    }


    val ssc = StreamingContext.getOrCreate(args(2),functionToCreateContext _)
    ssc.start()
    stopContext(ssc)
  }

}
