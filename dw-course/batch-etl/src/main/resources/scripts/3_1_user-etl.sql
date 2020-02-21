
--创建一张临时表
DROP TABLE IF EXISTS user_stage;
CREATE EXTERNAL TABLE user_stage (
    id INT,
    gender STRING,
    zip_code STRING,
    age INT,
    occupation STRING,
    last_modified STRING
)
ROW FORMAT DELIMITED
       FIELDS TERMINATED BY '\t'
LOCATION '/user/hadoop/movielens/user_stage';

--创建user_rating_history表
CREATE EXTERNAL TABLE IF NOT EXISTS user_history (
    id INT,
    gender STRING,
    zip_code STRING,
    age INT,
    occupation STRING,
    last_modified STRING
)
PARTITIONED BY (year INT, month INT, day INT)
STORED AS AVRO;

SET hive.exec.dynamic.partition.mode=nonstrict;

--将最新数据插入到user_history中，可能有重复数据
INSERT INTO TABLE user_history PARTITION(year, month, day)
SELECT *, year(last_modified) as year, month(last_modified) as month, day(last_modified) as day FROM user_stage;

--创建users外部表
CREATE EXTERNAL TABLE IF NOT EXISTS users (
    id INT,
    gender STRING,
    zip_code STRING,
    age INT,
    occupation STRING,
    last_modified STRING
)
STORED AS PARQUET;

--合并修改更新后的user
--首先插入不属于本次更新的数据
INSERT OVERWRITE TABLE users
SELECT users.id,users.gender,users.zip_code,users.occupation,users.last_modified
FROM users LEFT JOIN user_stage ON users.id = user_stage.id WHERE user_stage.id IS NULL;
--再插入属于本次更新的数据
INSERT INTO TABLE users SELECT id, gender, zip_code, age, occupation, last_modified FROM user_stage;