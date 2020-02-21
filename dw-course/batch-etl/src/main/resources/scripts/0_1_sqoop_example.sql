--需要使用下面命令启用hive metastore服务：
--nohup hive --service metastore > ~/bigdata/apache-hive-2.3.3-bin/logs/metastore.log 2>&1 &
CREATE DATABASE IF NOT EXISTS movielens;
use movielens;
DROP TABLE IF EXISTS movie_example;
CREATE TABLE movie_example(
id INT,
title STRING,
release_date STRING,
video_release_date STRING,
imdb_url STRING)
ROW FORMAT DELIMITED FIELDS TERMINATED BY ",";

LOAD DATA INPATH 'hdfs://master:9999/user/hadoop/movielens/movie' OVERWRITE INTO TABLE movie_example;

quit;