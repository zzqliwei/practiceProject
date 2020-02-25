package com.westar.sessionize.datamock

import java.util.Properties
import java.util.concurrent.TimeUnit

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

/**
  * bin/kafka-topic.sh --create --zookeeper master:2181 --replication-factor 1 --partitions 1 --topic session
  *
  * bin/kafka-console-consumer.sh --bootstrap-server master:9092 --topic session
  */
object SessionDataKafkaSender {
  def main(args: Array[String]): Unit = {
    val props = new Properties();
    props.put("bootstrap.servers","master:9092")
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("batch.size", "10")

    val producer = new KafkaProducer[Object,String](props)

    for(i <- 1 to 100){
      TimeUnit.MILLISECONDS.sleep(2000)
      producer.send(new ProducerRecord[Object,String]("session", SessionDataGenerator.getNextEvent))
    }

    producer.close()
  }

}
