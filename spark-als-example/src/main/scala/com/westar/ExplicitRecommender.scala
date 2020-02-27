package com.westar

import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.recommendation.{ALS, ALSModel}
import org.apache.spark.ml.tuning.{CrossValidator, ParamGridBuilder}
import org.apache.spark.sql.SparkSession

/**
  * 显式类型数据的推荐实现
  * spark-shell --master spark://master-dev:7077 --executor-memory 512M --executor-cores 1 --total-executor-cores 2
  */
object ExplicitRecommender {

  // 承载评分数据的case class
  case class Rating(userId: Int, movieId: Int, rating: Float, timestamp: Long)

  // 解析评分数据为Rating对象
  def parseRating(str: String): Rating = {
    val fields = str.split("::")
    assert(fields.size == 4)
    Rating(fields(0).toInt, fields(1).toInt, fields(2).toFloat, fields(3).toLong)
  }

  def main(args: Array[String]) {
    val spark = SparkSession
      .builder
      .appName("ExplicitRecommender")
      .getOrCreate()
    import spark.implicits._
    // 1、读取评分数据并解析为内存对象，得到一个评分的DataFrame
    val ratings = spark.read.textFile("hdfs://master:9999/user/hadoop-twq/als/sample_movielens_ratings.txt")
      .map(parseRating)
      .toDF()
    // 2、将整体数据集随机切分成训练数据集和测试数据集
    val Array(training, test) = ratings.randomSplit(Array(0.8, 0.2))
    training.cache()
    test.cache()
    // 3、构建ALS(交替最小二乘算法对象)
    val als = new ALS()
      .setMaxIter(5)
      .setRegParam(0.01)
      .setRank(10)
      .setUserCol("userId")
      .setItemCol("movieId")
      .setRatingCol("rating")
      .setPredictionCol("prediction")

    //4、利用ALS对训练数据集进行训练，生成模型
    val model = als.fit(training)

    //5、对生成的模型进行评估
    //5.1；利用测试数据喂给模型，生成推荐的结果
    model.setColdStartStrategy("drop")
    val predictions = model.transform(test)
    //5.2：对生成的推荐结果进行评估，计算均方根误差
    //((y1-x1)^2 + (y2 - x2)^2 + ... + (yn - xn)^2)/n
    // 其中y表示真实值，x表示测试数据集的预测值
    val evaluator = new RegressionEvaluator()
      .setMetricName("rmse")
      .setLabelCol("rating")
      .setPredictionCol("prediction")
    // 均方根误差越小越好，根据这个值来调节模型的参数
    val rmse = evaluator.evaluate(predictions)
    println(s"Root-mean-square error = $rmse")

    //6、推荐
    // 为每一个用户推荐10个产品
    val userRecs = model.recommendForAllUsers(10)
    // 为每一个产品推荐10个用户
    val movieRecs = model.recommendForAllItems(10)


    userRecs.show(truncate = false)
    movieRecs.show(truncate = false)

    // 使用k折交叉验证 （k-fold cross validation）来选择最佳的参数
    // 举个例子，例如10折交叉验证(10-fold cross validation)，将数据集分成10份，
    // 轮流将其中9份做训练1份做验证，10次的结果的均值作为对算法精度的估计。
    als.setColdStartStrategy("drop")
    val pipeline = new Pipeline().setStages(Array(als))
    val paramGrid = new ParamGridBuilder()
      .addGrid(als.rank, Array(10, 20))
      .addGrid(als.regParam, Array(0.05, 0.80))
      .build()
    // CrossValidator 需要一个Estimator,一组Estimator ParamMaps, 和一个Evaluator.
    // （1）Pipeline作为Estimator;
    // （2）定义一个RegressionEvaluator作为Evaluator，并将评估标准设置为“rmse”均方根误差
    // （3）设置ParamMap
    // （4）设置numFolds
    val cv = new CrossValidator()
      .setEstimator(pipeline)
      .setEvaluator(new RegressionEvaluator()
        .setLabelCol("rating")
        .setPredictionCol("prediction")
        .setMetricName("rmse"))
      .setEstimatorParamMaps(paramGrid)
      .setNumFolds(3) //3折交叉验证(3-fold cross validation)

    val cvModel = cv.fit(training)
    cvModel.bestModel

    val predictions2 = cvModel.transform(test)
    val evaluator2 = new RegressionEvaluator()
      .setMetricName("rmse")
      .setLabelCol("rating")
      .setPredictionCol("prediction")
    val rmse2 = evaluator2.evaluate(predictions2)
    println(s"Root-mean-square error = $rmse2")



    spark.stop()
  }
}
