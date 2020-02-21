#!/usr/bin/env bash
##将mysql中带有电影种类信息的电影数据导入到hdfs中
sqoop import --connect jdbc:mysql://master:3306/movie \
--username root --password WESTAR@soft1 \
--query 'SELECT movie.*,group_concat(genre.name) FROM movie
JOIN movie_genre ON (movie.id = movie_genre.movie_id)
JOIN genre ON(movie_genre.genre_id=genre.id)
WHERE $CONDITIONS
GROUP BY movie.id' \
--split-by movie.id \
--fields-terminated-by "\t" \
--target-dir /user/hadoop/movielens/movie \
--delete-targer-dir

## 将导入到HDFS中的电影数据导入到Hive表中(以parquet的方式存储)
hive f /home/hadoop/dw-course/1_1_movie-etl.sql

## 直接使用sqoop将mysql中的电影数据导入到hive表中
sqoop import --connect jdbc:mysql://master:3306/movie \
--username root --password WESTAR@soft1 \
--query 'SELECT movie.*,group_concat(genre.name) FROM movie
JOIN movie_genre ON (movie.id = movie_genre.movie_id)
JOIN genre ON(movie_genre.genre_id=genre.id)
WHERE $CONDITIONS
GROUP BY movie.id' \
-m 1 \
--as-parquetfile \
--hive-import \
--hive-overwrite \
--target-dir /user/hive/warehouse/movielens.db/movie2 \
--hive-database movielens \
--hive-table

## 对比movie和movie2的表结构
SHOW CREATE TABLE movie;
SHOW CREATE TABLE movie2;




