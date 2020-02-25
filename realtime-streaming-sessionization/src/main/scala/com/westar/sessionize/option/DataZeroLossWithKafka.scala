package com.westar.sessionize.option

import com.westar.sessionize.StreamingStopper
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}
/**
  *  自己控制消费的kafka的offsets
  *  优点：程序可以随意的升级，不会影响到程序的运行
  */
object DataZeroLossWithKafka extends StreamingStopper{
  def main(args: Array[String]): Unit = {
    val sparkConf = new SparkConf()
      .setAppName("DataZeroLossWithKafka")
      .set("spark.cleaner.ttl","12000")

    //本地测试

    sparkConf.setMaster("local[2]")

    val sc = new SparkContext(sparkConf)
    val ssc = new StreamingContext(sc,Seconds(10))

    // 通过direct的方式连接Kafka
    val topicsSet = args(0).split(",").toSet
    val brokers = args(1)
    val kafkaParams = Map[String, Object]("bootstrap.servers" -> brokers, "group.id" -> "group",
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

    val zkHosts = args(0)
    val zkPath = args(1)
    val lines:DStream[String]  = KafkaUtils.createDirectStream[Object,String](ssc, LocationStrategies.PreferConsistent,
      ConsumerStrategies.Subscribe[Object,String](topicsSet,kafkaParams)).map(_.value())

    // 第二种
    lines.foreachRDD { rdd =>
      val offsetRanges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges
      //do some transformation with rdd

      // output result to data store
      //write hbase....

      // some time later, after outputs have completed
      lines.asInstanceOf[CanCommitOffsets].commitAsync(offsetRanges)
    }


    ssc.start()
    stopContext(ssc)


  }

}
