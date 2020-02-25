package com.westar.sessionize.datamock

import java.text.SimpleDateFormat
import java.util.ArrayList

import scala.util.Random

/**
  * 模拟数据生成
  * 66.249.238.117 - - [2020-02-25 12:27:48] "GET /bar.html HTTP/1.1" 200 11179 "-" "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"
  */
object SessionDataGenerator {

  def main(args: Array[String]): Unit = {
    println(getNextEvent)
  }

  val r = new Random

  val numberOfUsers = 100000

  val intervalLength = 1000000

  val userbeingActivePercentage = 0.15

  val webSites = List("support.html", "about.html", "foo.html", "bar.html", "home.html", "search.html", "list.html", "help.html", "bar.html", "foo.html")

  var activeUserList = new ArrayList[Int]()

  var counter = 0
  var currentIntervalLength = (intervalLength * r.nextGaussian).toInt
  val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

  def getNextEvent():String = {
    if(counter == 0 || counter % currentIntervalLength == 0){
      //We are at the end of an interval
      currentIntervalLength = (intervalLength * r.nextGaussian).toInt

      activeUserList = new ArrayList[Int]()

      for(i <- 1 to numberOfUsers){
        if (Math.abs(r.nextGaussian) < userbeingActivePercentage) {
          activeUserList.add(i)
        }
      }
    }

    counter += 1

    val user = activeUserList.get(r.nextInt(activeUserList.size))
    val ipPart3 = user / 256
    val ipPart4 = user % 256

    "66.249." + ipPart3 + "." + ipPart4 + " - - [" + dateFormat.format(System.currentTimeMillis()) + "] \"GET /" + webSites(r.nextInt(10)) + " HTTP/1.1\" 200 11179 \"-\" \"Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)\""


  }

}
