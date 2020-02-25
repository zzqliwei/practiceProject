package com.westar.sessionize.kafka

import java.util

import com.westar.sessionize.kafka.stores.ZooKeeperOffsetsStore
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategy}

import scala.collection.JavaConversions._

/**
  *  zkCli.sh -server master:2181
  *  create /real_time_session ""
  */
object KafkaSource {
  def createDirectStream[K,V](ssc:StreamingContext,locationStrategy: LocationStrategy,
                              kafkaParams:Map[String,Object],zkHosts:String,zkPath:String,topic:String):DStream[ConsumerRecord[K,V]] = {
    val offsetsStore = new ZooKeeperOffsetsStore(zkHosts,zkPath)
    //读取数据
    val storesOffsets = offsetsStore.readOffsets(topic)
    val topicSet = Set(topic)

    //存在storesOffsets，则进行读取，不存在，则从头开始读取
    val kafkaStream = storesOffsets match {
      case Some(fromOffsets) =>
        KafkaUtils.createDirectStream[K,V](ssc,locationStrategy,ConsumerStrategies.Subscribe[K,V](util.Arrays.asList(topic),kafkaParams,fromOffsets))
      case None =>
        KafkaUtils.createDirectStream[K,V](ssc,locationStrategy,ConsumerStrategies.Subscribe[K,V](topicSet,kafkaParams))
    }
    //保存rdd
    kafkaStream.foreachRDD(rdd => offsetsStore.saveOffsets(topic,rdd))
    kafkaStream
  }
}
