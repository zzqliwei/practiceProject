package com.westar.prepare

import java.io.{File, PrintWriter}
import java.text.SimpleDateFormat
import java.util.Date

import scala.collection.mutable
import scala.io.Source

/**
 * 数据准备
 */
object InitSQLGen {
  def main(args: Array[String]): Unit = {
   val df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    //1、genres
    val genres = Source.fromFile("dw-course/batch-etl/src/main/resources/ml-100k/u.genre")
      .getLines()
      .map(line =>{
        val tmp = line.split("\\|")
        Genre(tmp(1).toInt,tmp(0))
      })
    val genreWriter = new PrintWriter(new File("dw-course/batch-etl/src/main/resources/sql/genres_init.sql"))

    genreWriter.println("USE movie;")
    genreWriter.println("--插入数据 id,name")
    genres.foreach(g =>{
      genreWriter.println(s"""insert into genre values(${g.id}, "${g.name}");""")
    })

    genreWriter.close()

    //2、occupations
    val occupationIdMap = new mutable.HashMap[String, Int]()
    val occupations = Source.fromFile("dw-course/batch-etl/src/main/resources/ml-100k/u.occupation").getLines().zipWithIndex.map { case (name, index) =>
      occupationIdMap += (name -> index)
      Occupation(index, name)
    }
    val occupationsWriter = new PrintWriter(new File("dw-course/batch-etl/src/main/resources/sql/occupations_init.sql"))
    occupationsWriter.println("USE movie;")
    occupationsWriter.println("--插入数据 index,name")
    occupations.foreach(o => {
      occupationsWriter.println(s"""insert into occupation values(${o.id}, "${o.name}");""")
    })
    occupationsWriter.close()

    //3、movies
    val movieInfos = Source.fromFile("dw-course/batch-etl/src/main/resources/ml-100k/u.item").getLines().map(line => {
      val tmp = line.split("\\|")
      val movieId = tmp(0).toInt
      val movieIdgenreIds = tmp.takeRight(19).zipWithIndex.flatMap { case (ge, index) =>
        if (ge.toInt == 1) {
          Some(index)
        } else None
      }.map(MovieGenre(movieId, _))
      (Movie(movieId, tmp(1), tmp(2), tmp(3), tmp(4)), movieIdgenreIds)
    })

    val moviesWriter = new PrintWriter(new File("dw-course/batch-etl/src/main/resources/sql/movies_init.sql"))
    moviesWriter.println("USE movie;")
    moviesWriter.println("--插入数据 id,name,releaseData,videoReleaseData,imdbUrl")

    val movieGenresWriter = new PrintWriter(new File("dw-course/batch-etl/src/main/resources/sql/movieGenres_init.sql"))
    movieGenresWriter.println("USE movie;")
    movieGenresWriter.println("--插入数据 movieId,genreId")
    movieInfos.foreach { case (m, mgs) =>
      val videoReleaseData = if (m.videoReleaseData.trim.length == 0) null else s"""STR_TO_DATE("${m.videoReleaseData}", "%d-%M-%Y")"""
      val releaseData = if (m.releaseData.trim.length == 0) null else s"""STR_TO_DATE("${m.releaseData}", "%d-%M-%Y")"""
      moviesWriter.println(
        s"""insert into movie values(${m.id}, "${m.name}", ${releaseData}, ${videoReleaseData}, "${m.imdbUrl}");""".stripMargin)
      mgs.foreach { mg =>
        movieGenresWriter.println(s"""insert into movie_genre values(${mg.movieId}, ${mg.genreId});""")
      }
    }
    moviesWriter.close()
    movieGenresWriter.close()

    val userWriter = new PrintWriter(new File("dw-course/batch-etl/src/main/resources/sql/users_init.sql"))
    userWriter.println("USE movie;")
    movieGenresWriter.println("--插入数据 index,gender,occupation,age,occupationId,当前时间")
    Source.fromFile("dw-course/batch-etl/src/main/resources/ml-100k/u.user")
      .getLines().foreach(line => {
      val tmp = line.split("\\|")
      userWriter.println(s"""insert into user values(${tmp(0).toInt}, '${tmp(2)}', "${tmp(4)}", ${tmp(1).toInt}, ${occupationIdMap.getOrElse(tmp(3), -1)}, NOW());""")
    })
    userWriter.close()

    val ratingsWriter = new PrintWriter(new File("dw-course/batch-etl/src/main/resources/sql/ratings_init.sql"))
    ratingsWriter.println("USE movie;")
    ratingsWriter.println("--插入数据 index,userid,movieId,rating,dt")
    Source.fromFile("dw-course/batch-etl/src/main/resources/ml-100k/u.data").getLines().zipWithIndex.foreach { case (line, index) =>
      val tmp = line.split("\t")
      val ts = tmp(3).toLong
      val dt = s"""STR_TO_DATE("${df.format(new Date(ts))}", "%Y-%m-%d %T")"""
      ratingsWriter.println(s"""insert into user_rating values(${index}, ${tmp(0).toInt}, ${tmp(1).toInt}, ${tmp(2).toInt}, ${dt});""")
    }
    ratingsWriter.close()

  }

}

case class Genre(id: Int, name: String)

case class Occupation(id: Int, name: String)

case class Movie(id: Int, name: String, releaseData: String, videoReleaseData: String, imdbUrl: String)

case class MovieGenre(movieId: Int, genreId: Int)

case class User(id: Int, age: Int, gender: String, occupation: String, zipcode: String)

case class Rating(userId: Int, movieId: Int, rating: Int, ts: Long)
