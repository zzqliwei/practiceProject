#!/usr/bin/env bash

# the two most important settings:
# 取决于如下因素：
# 1：每一秒接收到的events，尤其是在高峰时间
# 2：数据源的缓冲能力
# 3：可以忍受的最大的滞后时间
# 我们通过将我们的Streaming程序在准生产环境中跑几天来确定以上的因素
# 进而确定我们的executors的个数
num_executors=6
# 取决于如下因素：
# 1：每一个batch需要处理的数据的大小
# 2：transformations API的种类，如果使用的transformations需要shuffle的话，则需要的内存更大一点
# 3：使用状态Api的话，需要的内存更加大一点，因为需要内存缓存每一个key的状态
executor_memory=6g

# 每一个executor配置3到5个cores是比较好的，因为3到5个并发写HDFS是最优的
# see http://blog.cloudera.com/blog/2015/03/how-to-tune-your-apache-spark-jobs-part-2/
executor_cores=3

# backpressure
receiver_max_rate=100
receiver_initial_rate=30

spark-submit --master yarn --deploy-mode cluster \
  --name "Real_Time_SessionizeData" \
  --class com.westar.sessionize.SessionizeData \
  --driver-memory 2g \
  --num-executors ${num_executors} --executor-cores ${executor_cores} --executor-memory ${executor_memory} \
  --queue "realtime_queue" \
  --files "hdfs:master:8020/user/hadoop/real-time-analysis/log4j-yarn.properties" \
  --conf spark.driver.extraJavaOptions=-Dlog4j.configuration=log4j-yarn.properties \
  --conf spark.executor.extraJavaOptions=-Dlog4j.configuration=log4j-yarn.properties \
  --conf spark.serializer=org.apache.spark.serializer.KryoSerializer `# Kryo Serializer is much faster than the default Java Serializer`\
  --conf spark.locality.wait=10 `# 减少Spark Delay Scheduling从而提高数据处理的并行度, 默认是3000ms` \
  --conf spark.task.maxFailures=8 `# 增加job失败前最大的尝试次数, 默认是4`\
  --conf spark.ui.killEnabled=false `# 禁止从Spark UI上杀死Spark Streaming的程序`\
  --conf spark.logConf=true `# 在driver端打印Spark Configuration的日志`\
`# SPARK STREAMING CONFIGURATION` \
  --conf spark.streaming.blockInterval=200  `# 生成数据块的时间, 默认是200ms`\
  --conf spark.streaming.backpressure.enabled=true `# 打开backpressure的功能`\
  --conf spark.streaming.kafka.maxRatePerPartition=${receiver_max_rate}  `# direct模式读取kafka每一个分区数据的速度`\
`# YARN CONFIGURATION` \
  --conf spark.yarn.driver.memoryOverhead=512 `# Set if --driver-memory < 5GB`\
  --conf spark.yarn.executor.memoryOverhead=1024 `# Set if --executor-memory < 10GB`\
  --conf spark.yarn.maxAppAttempts=4  `# Increase max application master attempts`\
  --conf spark.yarn.am.attemptFailuresValidityInterval=1h  `# Attempt counter considers only the last hour`\
  --conf spark.yarn.max.executor.failures=$((8 * ${num_executors}))  `# Increase max executor failures`\
  --conf spark.yarn.executor.failuresValidityInterval=1h `# Executor failure counter considers only the last hour`\
  --driver-java-options hdfs://master:8020/user/hadoop/real-time-analysis/output real_time_session s hdfs://master:8020/user/hadoop/real-time-analysis/checkpoint session master:9092
/home/hadoop/real-time-analysis/realtime-streaming-sessionization-1.0-SNAPSHOT-jar-with-dependencies.jar \
hdfs://master:8020/user/hadoop/real-time-analysis/output real_time_session s hdfs://master:8020/user/hadoop/real-time-analysis/checkpoint session master:9092