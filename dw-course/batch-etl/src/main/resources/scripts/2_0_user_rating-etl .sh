#!/usr/bin/env bash
## 启动metastore
sqoop metastore &

## 启动之后的metastore的数据默认存储在~/.sqoop文件目录下
##如果想改变这个路径的话，需要设置$SQOOP_HOME/conf/sqoop-site.xml中的参数sqoop.metastore.server.location

## 删除名为user_rating_import的sqoop job
sqoop job --delete user_rating_import --meta-connect jdbc:hsqldb:hsql://master:16000/sqoop

## --meta-connect的端口也是在$SQOOP_HOME/conf/sqoop-site.xml配置的，参数为：sqoop.metastore.server.port
## 默认端口为16000

##创建一个sqoop job
##增量的将user_rating的数据从mysql中导入到hdfs中
sqoop job --create user_rating_import
--meta-connect jdbc:hsqldb:hsql://master:16000/sqoop \
--import --connect jdbc:mysql://master:3306/movie \
--username root -password WESTAR@soft1 \
--table user_rating -m 5 --split-by dt_time \
--incremental append --check-column dt_time \
--fields-terminated-by "\t" \
--target-dir /user/hadoop/movielens/user_rating_stage

## 执行user_rating_import任务
hadoop fs -rm -r /user/hadoop/movielens/user_rating_stage
sqoop job --exec user_rating_import --meta-connect jdbc:hsqldb:hsql://master:16000/sqoop

## 如果报错：java.sql.SQLException: Access denied for user 'root'@'master-dev' (using password: YES)
## 则执行：GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' IDENTIFIED BY 'root' WITH GRANT OPTION;



