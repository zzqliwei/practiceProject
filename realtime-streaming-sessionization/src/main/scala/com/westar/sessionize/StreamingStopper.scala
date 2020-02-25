package com.westar.sessionize

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.streaming.StreamingContext

trait StreamingStopper {
  //每10s检查一下用于关闭程序的hdfs文件是否存在
  val checkIntervalMillis = 10000
  //是否停止程序，默认不停止程序
  var isStopped = false

  //检测文件路径
  val shutdownFilePath = Option(System.getProperty("web.streaming.shutdown.filepath"))
    .getOrElse(sys.error("web.streaming.shutdown.filepath can not be null"))

  //停止程序
  def stopContext(ssc:StreamingContext) = {
    while (!isStopped){
      //检测是否程序停止运行
      val isStoppedTemp = ssc.awaitTerminationOrTimeout(checkIntervalMillis);
      //关闭程序的检测文件已存在，且程序未停止
      if(!isStoppedTemp && isShutdownRequested){
        val stopSparkContext=  true
        val stopGracefully = true;
        isStopped = true
        //优雅停机，仅处理还在线程中的数据，不接受新的数据
        ssc.stop(stopSparkContext,stopGracefully)
      }
    }
  }

  def isShutdownRequested():Boolean = {
    val fs = FileSystem.get(new Configuration())
    fs.exists(new Path(shutdownFilePath))
  }

}
