Sqoop的功能：
1. 数据导入的功能，即将传统关系型数据库中的数据导入到HDFS中(支持全量和增量)
2. 数据导出的功能，即将HDFS中的数据导入到传统关系型数据库中(仅支持全量导出)

这篇文章只关注Sqoop的导出功能，对于导入功能可以参考微职位课程中的[全量导入](http://edu.51cto.com/center/course/lesson/index?id=274465)和[增量导入](http://edu.51cto.com/center/course/lesson/index?id=274466)

## 准备数据
我们可以通过下面的SQL在HDFS中准备数据，我们先准备3张Hive表：
1. 表avg_movie_rating_1：844、845、846这三部电影的平均评分是多少？
```sql
-- 在hive中执行
USE movielens;
CREATE TABLE avg_movie_rating_1 AS SELECT movie_id, ROUND(AVG(rating), 1) AS rating FROM user_rating WHERE movie_id in(844, 845, 846) GROUP BY movie_id;
```
在hive执行了上面的SQL后，表`avg_movie_rating_tmp`对应的数据文件`/user/hive/warehouse/movielens.db/avg_movie_rating_1/000000_0`中的数据内容如下：
```shell
## 可以将文件下载下下来看下，只有3条数据，每一条数据只有两列
## 每一列之间是通过Hive默认的隐藏分隔符\001分隔开的
8443.4
8453.5
8463.1
```
2. 表avg_movie_rating_2：1560、1561、1562这三部电影的平均评分是多少？
```sql
-- 在hive中执行
USE movielens;
CREATE TABLE avg_movie_rating_2 AS SELECT movie_id, ROUND(AVG(rating), 1) AS rating FROM user_rating WHERE movie_id in(1560, 1561, 1562) GROUP BY movie_id;
```
在hive执行了上面的SQL后，表`avg_movie_rating_tmp`对应的数据文件`/user/hive/warehouse/movielens.db/avg_movie_rating_2/000000_0`中的数据内容如下：
```shell
## 可以将文件下载下下来看下，只有3条数据，每一条数据只有两列
## 每一列之间是通过Hive默认的隐藏分隔符\001分隔开的
15602.8
15611.0
15621.0
```

3. 表avg_movie_rating_3：1560、1561、1600这三部电影的平均评分是多少？
```sql
-- 在hive中执行
USE movielens;
CREATE TABLE avg_movie_rating_3 AS SELECT movie_id, ROUND(AVG(rating + 0.5), 1) AS rating FROM user_rating WHERE movie_id in(1560, 1561, 1600) GROUP BY movie_id;
```
在hive执行了上面的SQL后，表`avg_movie_rating_tmp`对应的数据文件`/user/hive/warehouse/movielens.db/avg_movie_rating_3/000000_0`中的数据内容如下：
```shell
## 可以将文件下载下下来看下，只有3条数据，每一条数据只有两列
## 每一列之间是通过Hive默认的隐藏分隔符\001分隔开的
15603.3
15611.5
16004.3
```
## 数据全量导出
现在我们需要将上面的文件数据导入到MySQL表中，MySQL中的表名是`avg_movie_rating_tmp`，可以通过如下的SQL脚本在MySQL中建表:
```sql
-- 在mysql中创建表
-- 字段movie_id需要满足不为空且唯一的约束
USE movie;
CREATE TABLE avg_movie_rating_tmp(movie_id INT NOT NULL unique, rating DOUBLE);
```

### avg_movie_rating_1导出到MySQL中
我们可以使用下面的sqoop的命令将hive表`avg_movie_rating_1`对应HDFS中的数据导入到MySQL中：
```shell
sqoop export \
--connect jdbc:mysql://master:3306/movie \
--username root --password root \
--table avg_movie_rating_tmp \
--export-dir /user/hive/warehouse/movielens.db/avg_movie_rating_1 \
--m 2 \
--input-fields-terminated-by '\001' --lines-terminated-by '\n' 
```
执行上面的命令后，MySQL表`movie.avg_movie_rating_tmp`中有了如下的数据：
![sqoop导出1](https://note.youdao.com/yws/res/60023/82A98F07999C4638B0DD09BEA80689E2)
可见，我们成功的从HDFS中全量的将数据导出到MySQL数据库中

### avg_movie_rating_2导出到MySQL中
我们还是使用导出`avg_movie_rating_1`的命令，只是改了下HDFS的文件目录而已，如下
```shell
sqoop export --connect jdbc:mysql://master:3306/movie \
--username root --password root \
--table avg_movie_rating_tmp \
--export-dir /user/hive/warehouse/movielens.db/avg_movie_rating_2 \
--m 2 \
--input-fields-terminated-by '\001' --lines-terminated-by '\n' 
```
执行上面的命令后，MySQL表`movie.avg_movie_rating_tmp`中有了如下的数据：
![sqoop导出2](https://note.youdao.com/yws/res/60024/BD7EB90969DC48E7808F43738C40C991)
可见，表`avg_movie_rating_2`中HDFS中全量的将数据也成功导出到MySQL数据库中了

**小结一下：**
1. sqoop export默认的话是将HDFS中的数据append的方式插入到MySQL表中，每一条数据的导出都会被翻译成一条`INSERT`语句，将数据插入到MySQL数据库中
2. 但是如果你的MySQL表的某个字段有唯一的约束的话，那么用sqoop export的默认方式导出数据的话，则会报错了，我们接着往下看

### avg_movie_rating_3导出到MySQL中
我们尝试还是用上面的sqoop命令将表`avg_movie_rating_3`导入到MySQL表中
```shell
sqoop export --connect jdbc:mysql://master:3306/movie \
--username root --password root \
--table avg_movie_rating_tmp \
--export-dir /user/hive/warehouse/movielens.db/avg_movie_rating_3 \
--m 2 \
--input-fields-terminated-by '\001' --lines-terminated-by '\n' 
```
我们先看下MySQL中的结果：
![sqoop导出3](https://note.youdao.com/yws/res/60020/AFE4B103D9C842E7962A5632B8130E09)
看的出，数据是不对的，1560的值应该是3.3，1561的值应该是1.5
我们载看下执行的sqoop任务的日志，其实是有报错的：
![sqoop导出4](https://note.youdao.com/yws/res/60021/25EA272A95DD464FB0803CEAF6D71121)
具体怎么排查sqoop的报错信息，请参考：[sqoop错误信息排查步骤](http://note.youdao.com/noteshare?id=16d4b47ecf4b3648222d58e1c9a0f614&sub=68BF6CE9DA4E441697A4161FFB4F9FCB)

所以我们不能用上面的sqoop脚本命令来跑带有唯一约束的数据了，我们先将MySQL表`movie.avg_movie_rating_tmp`中的movie_id=1600的数据重新删除，然后重新来导出数据:
```sql
-- 在MySQL中执行：
delete from movie.avg_movie_rating_tmp where movie_id=1600;
```

现在我们在sqoop脚本中加上`--update-key movie_id --update-mode updateonly`来重新导出数据，脚本如下：
```shell
sqoop export --connect jdbc:mysql://master:3306/movie \
--username root --password root \
--table avg_movie_rating_tmp \
--export-dir /user/hive/warehouse/movielens.db/avg_movie_rating_3 \
--m 2 --update-key movie_id --update-mode updateonly \
--input-fields-terminated-by '\001' --lines-terminated-by '\n' 
```
Sqoop任务也没有报错，我们看下我们MySQL中的数据：
![sqoop导出10](https://note.youdao.com/yws/res/60022/7345598C0EB7433487E401026E56EF3E)
我们看到1560和1561的数据被更新，但是1600的数据没有导出到MySQL表中。这说明设置`--update-key movie_id --update-mode updateonly`后只是会执行`UPDATE`的操作，不会执行插入的操作，如果我们即想执行`UPDATE`操作，又想执行`INSERT`操作的话，我们需要这样配置了：`--update-key movie_id --update-mode allowinsert`，脚本如下：
```shell
sqoop export --connect jdbc:mysql://master:3306/movie \
--username root --password root \
--table avg_movie_rating_tmp \
--export-dir /user/hive/warehouse/movielens.db/avg_movie_rating_3 \
--m 2 --update-key movie_id --update-mode allowinsert \
--input-fields-terminated-by '\001' --lines-terminated-by '\n' 
```
执行上面的脚本后，我们看下mySQL中的数据：
![sqoop导出11](https://note.youdao.com/yws/res/60019/4E5BD815A1874D678B6AC245AAB63178)

### 总结
1. sqoop export默认的话是将HDFS中的数据append的方式插入到MySQL表中，每一条数据的导出都会被翻译成一条`INSERT`语句，将数据插入到MySQL数据库中
2.  但是如果你的MySQL表的某个字段有唯一的约束的话，那么用sqoop export的默认方式导出数据的话，则会报错了，我们需要设置参数`--update-key movie_id --update-mode updateonly`来根据字段`movie_id`来更新数据
3. 光光更新数据是不够的，我们现在需要设置`--update-key movie_id --update-mode allowinsert`参数，来达到如果数据存在则根据字段进行`UPDATE`，如果数据不存在的话则执行`INSERT`操作

在上面的sqoop导出脚本中，我们只重点讨论`--update-key`和`--update-mode`两个参数，其他的参数的含义请参考[Sqoop官网的sqoop export](http://sqoop.apache.org/docs/1.4.7/SqoopUserGuide.html#_literal_sqoop_export_literal)

## Sqoop如何实现增量导出数据
Sqoop只支持全量导出数据，如果想做增量导出数据的话，需要额外做一步操作，比如现在有这么一个需求：
> 将电影772的平均评分这条记录导入到MySQL中表movie.avg_movie_rating_tmp中

我们需要分以下几步来实现这个增量导出：
1. 使用HQL将电影772的平均评分的数据导入到一张临时表中，如下：
```sql
USE movielens;
CREATE TABLE avg_movie_rating_772 AS SELECT movie_id, ROUND(AVG(rating), 1) AS rating FROM user_rating WHERE movie_id = 772 GROUP BY movie_id;
```
2. 然后使用下面的sqoop脚本将表`avg_movie_rating_772`对应的数据导入到MySQL表中：
```shell
sqoop export --connect jdbc:mysql://master:3306/movie \
--username root --password root \
--table avg_movie_rating_tmp \
--export-dir /user/hive/warehouse/movielens.db/avg_movie_rating_772 \
--m 2 --update-key movie_id --update-mode allowinsert \
--input-fields-terminated-by '\001' --lines-terminated-by '\n' 
```
3. 查看MySQL数据库表，看看数据是否进来：
![sqoop导出12](https://note.youdao.com/yws/res/60018/1CB087C40FE64E18A01F024005DC9DA8)

**对于上面的第一步可以是任何的计算逻辑，除了HQL也可以使用Spark等技术，目的只有一个：就是计算出需要增量导出的数据，然后按照一定的规则存储在HDFS中即可**