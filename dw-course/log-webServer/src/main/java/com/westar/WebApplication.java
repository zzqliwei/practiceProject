package com.westar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/*
启动log-webServer的命令：
1、日志一直放在同一个文件中
java -DLOG_HOME="/home/hadoop/dw-course/streaming-etl/logs"  \
-jar log-web-1.0-SNAPSHOT.jar \
--server.port=8090 \
--logging.config=/home/hadoop/dw-course/streaming-etl/logback/allfile_logback.xml

2、日志每分钟一个文件
java -DLOG_HOME="/home/hadoop/dw-course/streaming-etl/logs/minute"  \
-jar log-web-1.0-SNAPSHOT.jar \
--server.port=8090 \
--logging.config=/home/hadoop/dw-course/streaming-etl/logback/minute_logback.xml

2、日志每天一个文件，启动两个log-webServer
java -DLOG_HOME="/home/hadoop/dw-course/streaming-etl/logs/daily"  \
-jar log-web-1.0-SNAPSHOT.jar \
--server.port=8090 \
--logging.config=/home/hadoop/dw-course/streaming-etl/logback/minute_logback.xml

java -DLOG_HOME="/home/hadoop/dw-course/streaming-etl/logs/daily2"  \
-jar log-web-1.0-SNAPSHOT.jar \
--server.port=8091 \
--logging.config=/home/hadoop/dw-course/streaming-etl/logback/minute_logback.xml
 */

@SpringBootApplication
public class WebApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebApplication.class, args);
    }
}
