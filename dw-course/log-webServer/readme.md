访问日志的生成器
全天
每天
每分钟

1、通过 /user/action/json 接收到日志信息并通过kafka的user-action主题发送消息
2、通过 /user/action 直接将日志信息写入到文件中


/*
启动log-webServer的命令：
1、日志一直放在同一个文件中
java -DLOG_HOME="/home/hadoop-twq/dw-course/streaming-etl/logs"  \
-jar log-web-1.0-SNAPSHOT.jar \
--server.port=8090 \
--logging.config=/home/hadoop-twq/dw-course/streaming-etl/logback/allfile_logback.xml

2、日志每分钟一个文件
java -DLOG_HOME="/home/hadoop-twq/dw-course/streaming-etl/logs/minute"  \
-jar log-web-1.0-SNAPSHOT.jar \
--server.port=8090 \
--logging.config=/home/hadoop-twq/dw-course/streaming-etl/logback/minute_logback.xml

2、日志每天一个文件，启动两个log-webServer
java -DLOG_HOME="/home/hadoop-twq/dw-course/streaming-etl/logs/daily"  \
-jar log-web-1.0-SNAPSHOT.jar \
--server.port=8090 \
--logging.config=/home/hadoop-twq/dw-course/streaming-etl/logback/minute_logback.xml

java -DLOG_HOME="/home/hadoop-twq/dw-course/streaming-etl/logs/daily2"  \
-jar log-web-1.0-SNAPSHOT.jar \
--server.port=8091 \
--logging.config=/home/hadoop-twq/dw-course/streaming-etl/logback/minute_logback.xml
 */
