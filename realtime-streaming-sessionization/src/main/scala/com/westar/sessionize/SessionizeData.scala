package com.westar.sessionize

import java.text.SimpleDateFormat
import java.util.Date

import com.westar.sessionize.hbase.HBaseContext
import com.westar.sessionize.kafka.KafkaSource
import org.apache.hadoop.hbase.client.Put
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.{HBaseConfiguration, TableName}
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.kafka010.LocationStrategies
import org.apache.spark.streaming.{Seconds, State, StateSpec, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}

import scala.collection.immutable.HashMap

/**
  * spark-submit --master yarn --deploy-mode cluster \
  * --name "Real_Time_SessionizeData" \
  * --class com.westar.sessionize.SessionizeData \
  * --driver-memory 2g \
  * --num-executors ${num_executors} --executor-cores ${executor_cores} --executor-memory ${executor_memory} \
  * --queue "realtime_queue" \
  * --files "hdfs:master:8020/user/hadoop/real-time-analysis/log4j-yarn.properties" \
  * --conf spark.driver.extraJavaOptions=-Dlog4j.configuration=log4j-yarn.properties \
  * --conf spark.executor.extraJavaOptions=-Dlog4j.configuration=log4j-yarn.properties \
  * --conf spark.serializer=org.apache.spark.serializer.KryoSerializer `# Kryo Serializer is much faster than the default Java Serializer`\
  * --conf spark.locality.wait=10 `# 减少Spark Delay Scheduling从而提高数据处理的并行度, 默认是3000ms` \
  * --conf spark.task.maxFailures=8 `# 增加job失败前最大的尝试次数, 默认是4`\
  * --conf spark.ui.killEnabled=false `# 禁止从Spark UI上杀死Spark Streaming的程序`\
  * --conf spark.logConf=true `# 在driver端打印Spark Configuration的日志`\
  * `# SPARK STREAMING CONFIGURATION` \
  * --conf spark.streaming.blockInterval=200  `# 生成数据块的时间, 默认是200ms`\
  * --conf spark.streaming.backpressure.enabled=true `# 打开backpressure的功能`\
  * --conf spark.streaming.kafka.maxRatePerPartition=${receiver_max_rate}  `# direct模式读取kafka每一个分区数据的速度`\
  * `# YARN CONFIGURATION` \
  * --conf spark.yarn.driver.memoryOverhead=512 `# Set if --driver-memory < 5GB`\
  * --conf spark.yarn.executor.memoryOverhead=1024 `# Set if --executor-memory < 10GB`\
  * --conf spark.yarn.maxAppAttempts=4  `# Increase max application master attempts`\
  * --conf spark.yarn.am.attemptFailuresValidityInterval=1h  `# Attempt counter considers only the last hour`\
  * --conf spark.yarn.max.executor.failures=$((8 * ${num_executors}))  `# Increase max executor failures`\
  * --conf spark.yarn.executor.failuresValidityInterval=1h `# Executor failure counter considers only the last hour`\
  * --driver-java-options hdfs://master:8020/user/hadoop/real-time-analysis/output real_time_session s hdfs://master:8020/user/hadoop/real-time-analysis/checkpoint session master:9092
  * /home/hadoop/real-time-analysis/realtime-streaming-sessionization-1.0-SNAPSHOT-jar-with-dependencies.jar \
  * hdfs://master:8020/user/hadoop/real-time-analysis/output real_time_session s hdfs://master:8020/user/hadoop/real-time-analysis/checkpoint session master:9092
  */
object SessionizeData extends StreamingStopper {
  //文件输出的参数
  private val OUTPUT_ARG = 0
  //hbase 表的名称参数
  private val HTABLE_ARG = 1
  //hFamily
  private val HFAMILY_ARG = 2
  //检查点文件路径
  private val CHECKPOINT_DIR_ARG = 3
  //其他参数
  private val FIXED_ARGS = 4

  def main(args: Array[String]): Unit = {
    if (args.length == 0) {
      println("SessionizeData {outputDir} {table} {family}  {hdfs checkpoint directory} {zookeeper brokers}")
      return
    }
    val SESSION_TIMEOUT = (60000 * 0.5).toInt
    val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    val TOTAL_SESSION_TIME = "TOTAL_SESSION_TIME"
    val UNDER_A_MINUTE_COUNT = "UNDER_A_MINUTE_COUNT"
    val ONE_TO_TEN_MINUTE_COUNT = "ONE_TO_TEN_MINUTE_COUNT"
    val OVER_TEN_MINUTES_COUNT = "OVER_TEN_MINUTES_COUNT"
    val NEW_SESSION_COUNTS = "NEW_SESSION_COUNTS"
    val TOTAL_SESSION_COUNTS = "TOTAL_SESSION_COUNTS"
    val EVENT_COUNTS = "EVENT_COUNTS"
    val DEAD_SESSION_COUNTS = "DEAD_SESSION_COUNTS"
    val TOTAL_SESSION_EVENT_COUNTS = "TOTAL_SESSION_EVENT_COUNTS"

    val outputDir = args(OUTPUT_ARG)
    val hTableName = args(HTABLE_ARG)
    val hFamily = args(HFAMILY_ARG)
    val checkpointDir = args(CHECKPOINT_DIR_ARG)

    val sparkConf = new SparkConf()
      .setAppName("SessionizeData")
      .set("spark.cleaner.ttl", "12000")

    //本地测试
    //sparkConf.setMaster("local[2]")

    val sc = new SparkContext(sparkConf)
    val ssc = new StreamingContext(sc, Seconds(10))

    // 通过direct的方式连接Kafka
    val topicsSet = args(FIXED_ARGS).split(",").toSet
    val brokers = args(FIXED_ARGS + 1)
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
    //val lines: DStream[String] = KafkaUtils.createDirectStream[Object, String](ssc, LocationStrategies.PreferConsistent,
    //ConsumerStrategies.Subscribe[Object, String](topicsSet, kafkaParams)).map(_.value())
    val zkHosts = args(FIXED_ARGS + 2)
    val zkPath = args(FIXED_ARGS + 3)

    //从上次未处理的位置读取数据，会每隔10秒进行处理
    val lines = topicsSet.map(KafkaSource.createDirectStream[Object,String](ssc,LocationStrategies.PreferConsistent,kafkaParams,zkHosts,zkPath,_))
      .reduce( _ union _).map( _.value())

    //将原始事件日志解析转换成(ip, (startTime, endTime, record))
    val ipKeyLines = lines.map[(String, (Long, Long, String))](eventRecord =>{
      //对event进行预解析
      val time = dateFormat.parse(eventRecord.substring(eventRecord.indexOf('[') + 1, eventRecord.indexOf(']'))).getTime()
      val ipAddress = eventRecord.substring(0,eventRecord.indexOf(" "))
      //返回了两个time
      //第一个time表示这个ip的起始访问时间，第二个time表示这个ip的最后访问时间
      (ipAddress, (time, time, eventRecord))
    })

    //缓存数据
    ipKeyLines.cache()

    val updateStatbyOfSessions = (//(sessionStartTime, sessionFinishTime, countOfEvents)
      allEventsInAKey:Seq[(Long,Long,Long)],
      //(sessionStartTime, sessionFinishTime, countOfEvents, isNewSession)
      currentState: Option[(Long, Long, Long, Boolean)] ) => {
      val SESSION_TIMEOUT = (60000 * 0.5).toInt
      //针对一个key的最终返回的状态，如果该值为None的话，则表示需要从内存状态中删除这个key
      //This value contains four parts
      //(startTime, endTime, countOfEvents, isNewSession)
      var result: Option[(Long, Long, Long, Boolean)] = null


      //当前的Ip没有发送事件或当前状态不为空的场景：
      //如果超过了会话超时时间 再加上一个批处理的batch时间(即timeout + batch time)
      //没有接收到这个IP发出的event日志，则认为这个ip已经下线
      //可以将这个IP安全的从内存状态中删除掉
      if(allEventsInAKey.size == 0 && currentState.isDefined){
        if(System.currentTimeMillis() - currentState.get._2 > SESSION_TIMEOUT + 1100){
          result = None
        }else{
          //当前的ip的会话还没有超时，且当前状态不是一个新的会话，那么状态不变
          if(currentState.get._4 == false){
            result = currentState
          }else{
            //当前的ip的会话还没有超时，且当前状态是一个新的会话，那么将其设置为不是新会话
            result = Some((currentState.get._1,currentState.get._2,currentState.get._3,false))
          }
        }
      }


      //Now because we used the reduce function before this function we are
      //only ever going to get at most one event in the Sequence.
      //当前ip发送了event的场景：即allEventsInAKey不为空
      allEventsInAKey.foreach{ case (sessionStartTime, sessionFinishTime, countOfEvents) =>
          if(currentState.isEmpty){
            //如果当前的状态为空的话，则返回一个新的会话状态
            result = Some((sessionStartTime, sessionFinishTime, countOfEvents, true))
          }else{
            //如果ip发送的事件的时间减去当前状态的时间小于会话超时的时间，
            // 则继承上一个状态的信息，并返回一个新的状态
            if(sessionStartTime - currentState.get._2 < SESSION_TIMEOUT){
              result = Some((Math.min(sessionStartTime, currentState.get._1), //newStartTime
                Math.max(sessionFinishTime, currentState.get._2), //newFinishTime
                currentState.get._3 + countOfEvents, //newSumOfEvents
                false //This is not a new session
               ))
            }else{
              result = Some((sessionStartTime, sessionFinishTime, countOfEvents, true))
            }
          }
      }

      result
    }


    //计算最新的会话信息 ipKeyLines 转计数
    val latestSessionInfo = ipKeyLines.map[(String, (Long, Long, Long))]( a =>{
      //(ip, (startTime, endTime, record))
      (a._1, (a._2._1, a._2._2, 1))
    }).reduceByKey( (a,b) =>{
      //transform to (ipAddress, (lowestStartTime, MaxFinishTime, sumOfCounter))
      (Math.min(a._1,b._1), Math.max(a._2,b._2),  a._3 + b._3  )
    })
      .updateStateByKey(updateStatbyOfSessions)
    //返回的类型是(ipAddress, (sessionStartTime, sessionFinishTime, countOfEvents, isNewSession))
//    .mapWithState(stateSpec.timeout(Seconds(30))).stateSnapshots().filter(_._2 != null)

    //过滤出活跃的(即当前时间减去会话的结束时间小于30分钟)
    val onlyActiveSessions = latestSessionInfo.filter(t => System.currentTimeMillis() - t._2._2 < SESSION_TIMEOUT)

    //计算totalSessionTime, underAMinuteCount, oneToTenMinuteCount, overTenMinutesCount
    val totals = onlyActiveSessions.mapPartitions[(Long, Long, Long, Long)](it =>{
      var totalSessionTime: Long = 0
      var underAMinuteCount: Long = 0
      var oneToTenMinuteCount: Long = 0
      var overTenMinutesCount: Long = 0

      it.foreach { case (_, (sessionStartTime, sessionFinishTime, _, _)) =>
        val time = sessionFinishTime - sessionStartTime
        //所有会话的总共的时间
        totalSessionTime += time
        //小于1分钟的会话数
        if (time < 60000) {
          underAMinuteCount += 1
        } else if (time < 100000) {
          //大于等于1分钟但是小于10分钟的会话数
          oneToTenMinuteCount += 1
        } else {
          //大于10分钟的会话数
          overTenMinutesCount += 1
        }
      }
      Iterator((totalSessionTime,underAMinuteCount,oneToTenMinuteCount,overTenMinutesCount))
    },true).reduce((a,b) =>{
      //各个分区数据相加
      (a._1 + b._1,a._2 + b._2,a._3 + b._3,a._4+b._4)
    }).map[HashMap[String,Long]]( t=>HashMap(
      (TOTAL_SESSION_TIME, t._1),
      (UNDER_A_MINUTE_COUNT, t._2),
      (ONE_TO_TEN_MINUTE_COUNT, t._3),
      (OVER_TEN_MINUTES_COUNT, t._4)
    ))


    //计算新的会话数
    val newSessionCount = onlyActiveSessions.filter(t =>{
      //当前时间减去会话结束时间小于batch的时间 且 是新的会话
      System.currentTimeMillis() - t._2._2 < 1100 && t._2._4
    }).count()
      .map[HashMap[String, Long]](t => HashMap((NEW_SESSION_COUNTS, t)))

    //计算总共的会话数
    val totalSessionCount = onlyActiveSessions.count.
      map[HashMap[String, Long]](t => HashMap((TOTAL_SESSION_COUNTS, t)))

    //计算所有活跃会话中的总共的事件数
    val totalSessionEventCount:DStream[HashMap[String,Long]] = onlyActiveSessions.map( a=>a._2._3).reduce( _ + _)
      .map[HashMap[String, Long]](t => HashMap((TOTAL_SESSION_EVENT_COUNTS, t)))

    //计算所有的事件的数量
    val totalEventsCount: DStream[HashMap[String, Long]] = ipKeyLines.count
      .map[HashMap[String, Long]](t => HashMap((EVENT_COUNTS, t)))

    //计算已经死了的会话数
    val deadSessionsCount: DStream[HashMap[String, Long]] =latestSessionInfo.map( t=>{
      val gapTime = System.currentTimeMillis() - t._2._2
      gapTime > SESSION_TIMEOUT
    }).count
      .map[HashMap[String, Long]](t => HashMap((DEAD_SESSION_COUNTS, t)))

    //将所有的统计联合起来放在一个HashMap中
    val allCounts :DStream[HashMap[String,Long]] = newSessionCount.
      union(totalSessionCount).
      union(totals).
      union(totalEventsCount).
      union(deadSessionsCount).
      union(totalSessionEventCount).reduce((a,b) => b.++(a))

    //将数据保存到Hbase中
    val conf = HBaseConfiguration.create()
    val hbaseContext = new HBaseContext(sc,conf)

    //dstream  tableName
    hbaseContext.streamBulkPut[HashMap[String,Long]](
      allCounts,
      TableName.valueOf(hTableName),
      (t:HashMap[String,Long]) =>{
        //C.表示统计的是会话的Count
        //后缀是Long的最大值减去当前的时间，这样可以保证最新的数据排在表的最前面(利用了HBase的自动排序的功能)
        val put = new Put(Bytes.toBytes("C."+ (Long.MaxValue - System.currentTimeMillis())))

        t.foreach( kv =>put.addColumn(Bytes.toBytes(hFamily),Bytes.toBytes(kv._1),Bytes.toBytes(kv._2.toString)))

        if(!t.contains(TOTAL_SESSION_EVENT_COUNTS)){
          put.addColumn(Bytes.toBytes(hFamily), Bytes.toBytes(TOTAL_SESSION_EVENT_COUNTS), Bytes.toBytes(0L.toString))
        }
        put
      }
    )
    //Persist to HDFS
    //(ip, (startTime, endTime, record)) join (ip, (sessionStartTime, sessionFinishTime, countOfEvents, isNewSession))
    // => (ip, ((startTime, endTime, record), (sessionStartTime, sessionFinishTime, countOfEvents, isNewSession)))
    ipKeyLines.join(onlyActiveSessions).map(t=>{
      //Session root start time | Event message
      t._1 + "\t" + dateFormat.format(new Date(t._2._2._1)) + "\t" + t._2._1._3
    }).saveAsTextFiles(outputDir + "/session", "txt")

    ssc.checkpoint(checkpointDir)
    ssc.start()
    stopContext(ssc)
  }

  val stateSpec = StateSpec.function((key: String, value: Option[(Long, Long, Long)],
                                      currentState: State[(Long, Long, Long, Boolean)]) => {
    val SESSION_TIMEOUT = (60000 * 0.5).toInt
    var result: (Long, Long, Long, Boolean) = null

    if (currentState.isTimingOut()) {
      result = null
    } else {
      //Now because we used the reduce function before this function we are
      //only ever going to get at most one event in the Sequence.
      //当前ip发送了event的场景：
      value.foreach { case (sessionStartTime, sessionFinishTime, countOfEvents) =>
        if (currentState.getOption().isEmpty) {
          //If there was no value in the Stateful DStream then just add it
          //new, with a true for being a new session
          //如果当前的状态为空的话，则返回一个新的会话状态
          result = (sessionStartTime, sessionFinishTime, countOfEvents, true)
          currentState.update(result)
        } else {
          //如果ip发送的事件的时间减去当前状态的时间小于会话超时的时间，
          // 则继承上一个状态的信息，并返回一个新的状态
          if (sessionStartTime - currentState.get._2 < SESSION_TIMEOUT) {
            result = (
              Math.min(sessionStartTime, currentState.get._1), //newStartTime
              Math.max(sessionFinishTime, currentState.get._2), //newFinishTime
              currentState.get._3 + countOfEvents, //newSumOfEvents
              false //This is not a new session
            )
            currentState.update(result)
          } else {
            //否则删除老的会话状态，返回一个全新的会话状态
            result = (
              sessionStartTime, //newStartTime
              sessionFinishTime, //newFinishTime
              currentState.get._3, //newSumOfEvents
              true //new session
            )
            currentState.update(result)
          }
        }
      }
    }
    (key, result)
  })

}
