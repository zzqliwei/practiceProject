网站流量离线分析

# 四个模块的说明：

## tracker：主要用于采集用户访问网站行为的数据

## tracker-logServer：用于接收tracker发送过来的日志信息并且存储在本地文件中

## backend：用于数据的清洗和解析处理，最终入库到hive
    <modules>
        <module>weblog-preparser</module>
        <module>spark-preparse-etl</module>
        <module>weblog-parser</module>
        <module>IPLocation-parser</module>
        <module>metadata</module>
        <module>spark-sessionization-etl</module>
    </modules>

## frontend：用于数据的分析和可视化
