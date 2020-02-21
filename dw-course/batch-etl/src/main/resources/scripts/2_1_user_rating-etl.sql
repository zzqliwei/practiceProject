--需要使用命令启动hive metastore服务
--nohup hive --server metastore > ~/bigdata/apache-hive-2.3.3-bin/logs/metastore.log 2>&1 &

--创建一张临时表,用hdfs的文件
DROP TABLE IF EXISTS user_rating_stage;
CREATE EXTERNAL TABLE user_rating_stage(
id INT,
user_id INT,
movie_id INT,
rating INT,
dt_time STRING)
ROW FORMAT DELIMITED
 FIELDS TERMINATED BY "\t"
LOCATION '/user/hadoop/movielens/user_rating_stage';

--创建user_rating表
CREATE EXTERNAL TABLE IF NOT EXISTS user_rating(
id INT,
user_id INT,
movie_id INT,
rating INT,
dt_time STRING)
PARTITIONED BY (year INT,month INT,day INT)
STORED AS AVRO;

SET hive.exec.dynamic.partition.mode=nonstrict;
INSERT INTO TABLE user_rating PARTITION(year,month,day)
SELECT id, user_id, movie_id, rating, dt_time, year(dt_time) as year, month(dt_time) as month, day(dt_time) as day
FROM user_rating_stage;


--创建user_rating_fact表
CREATE EXTERNAL TABLE IF NOT EXISTS user_rating_fact(
id INT,
user_id INT,
movie_id INT,
rating INT,
dt_time STRING)
PARTITIONED BY (year INT,month INT,day INT)
STORED AS PARQUET ;

--user_rating有重复数据，则user_rating_fact中插入数据，所有去重数据
WITH t1 as (
    SELECT
        id,
        user_id,
        movie_id,
        rating,
        dt_time,
        year,
        month,
        day,
        ROW_NUMBER() OVER (PARTITION BY user_id, movie_id ORDER BY dt_time DESC) as rn
    FROM user_rating
)
INSERT OVERWRITE TABLE user_rating_fact PARTITION(year, month, day)
SELECT id, user_id, movie_id, rating, dt_time, year, month, day FROM t1 WHERE rn = 1;
