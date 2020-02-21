#!/usr/bin/env bash
##--password 明文放在命令中，不安全，别人通过history可以拿到密码
sqoop import --connect jdbc:mysql://master:3306/movie \
--username root --password WESTAR@soft1 \
--table movie -m 1 --delete-target-dir

##-P执行的时候在控制台输入密码，更加安全
sqoop import --connect jdbc:mysql://master:3306/movie \
--username root --P \
--table movie -m 1 --delete-target-dir

##--password-file 的方式更加方便且安全
##支持本地文件以及HDFS文件
##本地文件 touch /home/hadoop/.password
## echo -n "WESTAR@soft1" >> /home/hadoop/.password
##chmod 400 /home/hadoop/.password
sqoop import --connect jdbc:mysql://master:3306/movie \
--username root --password-file file:///home/hadoop/.password \
--table movie -m 1 --delete-target-dir

## hadoop fs -put ~/.password /user/hadoop
## hadoop fs -chmod 400 /user/hadoop/.password
sqoop import --connect jdbc:mysql://master:3306/movie \
--username root --password-file /user/hadoop/.password \
--table movie -m 1 --delete-target-dir

