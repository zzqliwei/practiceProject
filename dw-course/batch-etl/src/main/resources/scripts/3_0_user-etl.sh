#!/usr/bin/env bash
##启动metastore
sqoop metastore &

## 删除名为user_import的sqoop job
sqoop job --delete user_import --meta-connect jdbc:hsqldb:hsql://master:16000/sqoop

##创建一个sqoop job
## 增量将user的数据从mysql中导入到hdfs中
sqoop job --create user_import \
--meta-connect jdbc:hsqldb:hsql://master:16000/sqoop \
-- import --connect jdbc:mysql://master:3306/movie \
--username root --password WESTAR@soft1 \
-m 5 --split-by last_modified \
--query 'SELECT user.id, user.gender, user.zip_code, user.age, occupation.occupation, user.last_modified
FROM user JOIN occupation ON user.occupation_id = occupation.id WHERE $CONDITIONS' \
--fields-termenated-by "\t" \
--target-dir /user/hadoop/movielens/user_stage

##执行user_import任务
hadoop fs -rm -r /user/hadoop/movielens/user_stage
sqoop job --exec user_import --meta-connect jdbc:hsqldb:hsql://master:16000/sqoop

