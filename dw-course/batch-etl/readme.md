实现初始化数据sql生成

实现将mysql数据保存到hive
实现将hive数据保存到mysql

script/0_* 是将mysql数据保存到hdfs并实现密码安全
script/1_* 是将hdfs文件存到hive,通过外部表的形式
script/2_0 是sqoop增量添加movie数据 sqoop 删除、创建、执行
script/2_1 是hive全量添加user_rating数据
script/2_1 是测试插入user_rating数据，判定是否数据同步成功

script/3_* 是sqoop + hdfs + hive增量和变更user数据 sqoop 删除、创建、执行
            sqoop 获取增量数据并保存到hdfs中
            hive 处理增量和变更数据
script/4_* 是分析hive中的数据，并将数据保存到mysql

sqoop 同步数据步骤
    1、先删除sqoop job
    sqoop job --delete sqoop_job_name --meta-connect jdbc:hsqldb:hsql://master:16000/sqoop
    2、创建sqoop job
    sqoop job --create
    3、删除对应hdfs的文件
    hadoop fs -rm -r
    4、执行 sqoop job
    sqoop job --exec
然后执行hive同步数据步骤
    1、删除临时外部表
    DROP TABLE
    2、创建临时外部表
    CREATE EXTERNAL TABLE ##需要执行LOCATION
    3、创建临时外部分区表
    CREATE EXTERNAL TABLE
    PARTITIONED BY (year INT,month INT,day INT)
    STORED AS AVRO;
    4、SET hive.exec.dynamic.partition.mode=nonstrict;
    5、开始插入同步数据和分析

