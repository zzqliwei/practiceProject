package com.westar.sessionize.kafka.stores

import com.westar.sessionize.kafka.utils.Stopwatch
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.kafka.common.TopicPartition
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.kafka010.HasOffsetRanges
import org.slf4j.LoggerFactory

/**
  *  将Spark Streaming消费的kafka的offset信息保存到zookeeper中
  * @param zkHosts zookeeper的主机信息
  * @param zkPath offsets存储在zookeeper的路径
  */
class ZooKeeperOffsetsStore(zkHosts: String, zkPath: String) extends OffsetsStore with Serializable {
  private val logger = LoggerFactory.getLogger("ZooKeeperOffsetsStore")

  @transient
  private val client = CuratorFrameworkFactory.builder()
    .connectString(zkHosts)
    .connectionTimeoutMs(10000)
    .sessionTimeoutMs(10000)
    .retryPolicy(new ExponentialBackoffRetry(1000, 3))
    .build()
  client.start()

  /**
    *  从zookeeper上读取Spark Streaming消费的指定topic的所有的partition的offset信息
    * @param topic
    * @return
    */
  override def readOffsets(topic: String): Option[Map[TopicPartition, Long]] = {
    logger.info("Reading offsets from ZooKeeper")
    val stopwatch = new Stopwatch()

    val offsetsRangesStrOpt = Some(new String(client.getData.forPath(zkPath)))
    offsetsRangesStrOpt match {
      case Some(offsetsRangesStr) =>
        logger.info(s"Read offset ranges: ${offsetsRangesStr}")
        if (offsetsRangesStr.isEmpty) {
          None
        } else {
          val offsets = offsetsRangesStr.split(",")
            .map(s => s.split(":"))
            .map { case Array(partitionStr, offsetStr) => (new TopicPartition(topic, partitionStr.toInt) -> offsetStr.toLong) }
            .toMap

          logger.info("Done reading offsets from ZooKeeper. Took " + stopwatch)

          Some(offsets)
        }
      case _ =>
        logger.info("No offsets found in ZooKeeper. Took " + stopwatch)
        None
    }
  }

  /**
    *  将指定的topic的所有的partition的offset信息保存到zookeeper中
    * @param topic
    * @param rdd
    */
  override def saveOffsets(topic: String, rdd: RDD[_]): Unit = {
    logger.info("Saving offsets to ZooKeeper")
    val stopwatch = new Stopwatch()

    val offsetsRanges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges
    offsetsRanges.foreach(offsetRange => logger.info(s"Using ${offsetRange}"))

    val offsetsRangesStr = offsetsRanges.map(offsetRange => s"${offsetRange.partition}:${offsetRange.fromOffset}")
      .mkString(",") //partition1:220,partition2:320,partition3:10000
    logger.info(s"Writing offsets to ZooKeeper: ${offsetsRangesStr}")
    client.setData().forPath(zkPath, offsetsRangesStr.getBytes())
    //ZkUtils.updatePersistentPath(zkClient, zkPath, offsetsRangesStr)

    logger.info("Done updating offsets in ZooKeeper. Took " + stopwatch)
  }
}
