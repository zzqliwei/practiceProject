--需要使用下面的命令启动hive metastore服务：
--nohup hive --service metastore > ~/bigdata/apache-hive-2.3.3-bin/logs/metastore.log 2>&1 &
CREATE DATABASE IF NOT EXISTS movielens;
CREATE EXTERNAL TABLE IF NOT EXISTS movielens.movie(
id INT,
title STRING,
release_date STRING,
video_release_date STRING,
imdb_url STRING,
genres ARRAY<STRING>)
STORED AS parquet ;


CREATE EXTERNAL TABLE IF NOT EXISTS movielens.movie_temp(
id INT,
title STRING,
release_date STRING,
video_release_date STRING,
imdb_url STRING,
genres ARRAY<STRING>)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY "\t"
COLLECTION ITEMS TERMINATED BY ","
LOCATION '/user/hadoop/movielens/movie';

INSERT OVERWRITE TABLE movielens.movie SELECT * FROM movielens.movie_temp;

DROP TABLE movielens.movie_temp;

quit;




