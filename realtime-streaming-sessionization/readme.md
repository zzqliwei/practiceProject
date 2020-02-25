互联网用户访问日志实时分析

SessionizeData 
1、从kafka读取数据 ，若存在storesOffsets，则进行读取，不存在，则从头开始读取
2、程序是否关闭取决于hdfs中的文件
3、采用Hbase工具包，将数据保存到hbase


DataZeroLossWithKafka 
    自己控制消费的kafka的offsets
    优点：程序可以随意的升级，不会影响到程序的运行
 