package com.westar

import org.apache.spark.broadcast.Broadcast
import org.apache.spark.ml.recommendation.{ALS, ALSModel}
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}
import org.apache.spark.sql.functions._

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

/**
  *  spark-shell --master spark://master-dev:7077 --executor-memory 512M --executor-cores 1 --total-executor-cores 2
  *  隐式类型数据的推荐实现
  */
object ImplicitRecommender {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder().getOrCreate()

    spark.sparkContext.setCheckpointDir("hdfs://master:9999/user/hadoop-twq/als/tmp")

    val base = "hdfs://master:9999/user/hadoop-twq/als/"
    val rawUserArtistData = spark.read.textFile(base + "user_artist_data.txt")
    // 用户对艺术家的播放次数
    // user_artist_data.txt
    //  3 columns: userid artistid playcount
    rawUserArtistData.take(5).foreach(println)
    val rawArtistData = spark.read.textFile(base + "artist_data.txt")
    // 艺术家id以及名字
    // artist_data.txt
    //    2 columns: artistid artist_name
    rawArtistData.take(5).foreach(println)
    val rawArtistAlias = spark.read.textFile(base + "artist_alias.txt")
    // 艺术家名字修正表
    // artist_alias.txt
    //    2 columns: badid, goodid
    rawArtistAlias.take(5).foreach(println)

    val runRecommender = new ImplicitRecommender(spark)
    // 准备数据，其实就是了解数据，对数据进行解析转换
    runRecommender.preparation(rawUserArtistData, rawArtistData, rawArtistAlias)
    // 构建第一个模型
    runRecommender.model(rawUserArtistData, rawArtistData, rawArtistAlias)
    // 评估模型的质量，且计算出最佳的参数
    runRecommender.evaluate(rawUserArtistData, rawArtistAlias)
    // 利用最佳的参数，最终构建模型，基于模型做推荐
    runRecommender.recommend(rawUserArtistData, rawArtistData, rawArtistAlias)
  }
}

class ImplicitRecommender(private val spark: SparkSession) {

  import spark.implicits._

  def buildArtistById(rawArtistData: Dataset[String]): DataFrame = {
    rawArtistData.flatMap { line =>
      val (id, name) = line.span(_ != '\t')
      if (name.isEmpty) {
        None
      } else {
        try {
          Some((id.toInt, name.trim))
        } catch {
          case _: NumberFormatException => None
        }
      }
    }.toDF("id", "name")
  }

  def buildArtistAlias(rawArtistAlias: Dataset[String]): Map[Int, Int] = {
    rawArtistAlias.flatMap { line =>
      val Array(artist, alias) = line.split("\t")
      if (artist.isEmpty) {
        None
      } else {
        Some((artist.toInt, alias.toInt))
      }
    }.collect().toMap
  }

  def preparation(rawUserArtistData: Dataset[String],
                  rawArtistData: Dataset[String], rawArtistAlias: Dataset[String]): Unit = {

    // 1：Spark MLlib中的ALS算法，必须要求userId和itemId必须是数值型，并且是32位非负整数
    // 这意味着大于Integer.MAX_VALUE(即2147483647)的ID都是非法的
    // 我们需要看下我们的id是不是都符合这个要求，我们看一下最大最小的userId和artistId即可
    // 解析原始的用户播放次数数据，生成DataFrame
    val userArtistDF = rawUserArtistData.map { line =>
      val Array(user, artist, _*) = line.split(" ")
      (user.toInt, artist.toInt)
    }.toDF("user", "artist")
    // 计算最大最小的userId和artistId
    userArtistDF.agg(min("user"), max("user"), min("artist"), max("artist")).show()

    // 2：解析原始的艺术家数据，生成DataFrame
    rawArtistData.map { line =>
      // 这里span() 用第一个制表符将一行拆分成两部分，接着将第一部分解析为艺术家ID，
      // 剩余部分作为艺术家的名字（去掉了空白的制表符）
      val (id, name) = line.span(_ != '\t')
      (id.toInt, name.trim)
    }.count() // 这个方法会报错,使用如下的方法：
    rawArtistData.flatMap { line => // String => Option
      val (id, name) = line.span(_ != '\t')
      if (name.isEmpty) {
        None
      } else {
        try {
          Some((id.toInt, name.trim))
        } catch {
          case _: NumberFormatException => None
        }
      }
    }.count()
    // 将上面的解析逻辑放到一个方法中
    val artistById = buildArtistById(rawArtistData)

    // 3：解析原始的艺术家名字修正数据
    // 因为数据量不大，所以，我们可以将这个数据收集到driver端，并且转成Map
    rawArtistAlias.flatMap { line =>
      val Array(badId, goodId) = line.split("\t")
      if (badId.isEmpty) {
        None
      } else {
        Some((badId.toInt, goodId.toInt))
      }
    }.collect().toMap
    // 将上面的解析逻辑放到一个方法中
    val artistAlias = buildArtistAlias(rawArtistAlias)

    // 验证可不可以对名字进行修正
    val (badId, goodId) = artistAlias.head
    artistById.filter($"id".isin(badId, goodId)).show()

    // 将艺术家名字修正数据解析后broadcast到executor上
    val bArtistAlias = spark.sparkContext.broadcast(artistAlias)
    // 解析用户播放次数的原始数据
    rawUserArtistData.map { line =>
      val Array(userId, artistId, count) = line.split(" ").map(_.toInt)
      val finalArtistId = bArtistAlias.value.getOrElse(artistId, artistId)
      (userId, finalArtistId, count)
    }.toDF("user", "artist", "count")
    // 解析用户播放次数的原始数据
    val ratingData = buildCounts(rawUserArtistData, bArtistAlias)
    ratingData.show()
  }

  def buildCounts(rawUserArtistData: Dataset[String],
                  bArtistAlias: Broadcast[Map[Int, Int]]): DataFrame = {
    rawUserArtistData.map { line =>
      val Array(userId, artistId, count) = line.split(" ").map(_.toInt)
      val finalArtistId = bArtistAlias.value.getOrElse(artistId, artistId)
      (userId, finalArtistId, count)
    }.toDF("user", "artist", "count")
  }

  /**
    *  构建第一个模型
    * @param rawUserArtistData 用户播放次数的原始数据
    * @param rawArtistData 艺术家原始数据
    * @param rawArtistAlias  艺术家名字修正原始数据
    */
  def model(rawUserArtistData: Dataset[String],
            rawArtistData: Dataset[String], rawArtistAlias: Dataset[String]): Unit = {
    // 将艺术家名字修正数据解析后broadcast到executor上
    val bArtistAlias = spark.sparkContext.broadcast(buildArtistAlias(rawArtistAlias))
    // 将训练数据集缓存，因为需要对训练数据集进行迭代计算
    val trainData = buildCounts(rawUserArtistData, bArtistAlias).cache()
    // 利用训练数据集进行模型训练
    val model: ALSModel = new ALS()
      .setSeed(Random.nextLong()) // 随机种子数
      .setImplicitPrefs(true) // 我们这里的数据是隐式数据集
      .setRank(10)  // 隐语义因子的个数，默认是10
      .setRegParam(0.01) // 指定ALS的正则化参数，默认是1.0，这个值越大可以有效的防止过拟合，但是越大的话会影响矩阵分解的精度
      .setAlpha(1.0)  //  置信度的增长速度。对于这个参数的解释可以参考：https://amsterdam.luminis.eu/2016/12/04/alternating-least-squares-implicit-feedback-search-alpha/
      .setMaxIter(5) // 迭代的次数
      .setUserCol("user") // 用户列
      .setItemCol("artist") // 艺术家列
      .setRatingCol("count") // 播放次数
      .setPredictionCol("prediction") // 推荐值列
      .fit(trainData) // 模型训练  //这里也需要花点时间

    trainData.unpersist() // 模型训练完，则需要将训练数据从内存中清除

    // 查看用户特征
    // 用户与隐语义因子之间的关系
    model.userFactors.show(1, truncate = false)
    model.itemFactors.show(1, truncate = false)

    val userId = 2093760 // 给这个用户做推荐

    // 查看这个用户给播放了哪些艺术家的歌曲
    val existingArtistIds = trainData.filter($"user" === userId).select("artist").as[Int].collect()

    val artistById = buildArtistById(rawArtistData)
    // 查看这些艺术家的名字
    artistById.filter($"id" isin (existingArtistIds: _*)).show()
    // 给指定的用户推荐5个最合适的艺术家
    val topRecommendations = makeRecommendations(model, userId, 5)
    topRecommendations.show() // 计算需要花点时间
    // 看看推荐的艺术家的名字，都是演hip-hop的艺术家
    val recommendArtistIds = topRecommendations.select("artist").as[Int].collect()
    artistById.filter($"id" isin (recommendArtistIds: _*)).show()

    // 将模型中的用户特征和产品特征数据都从内存中释放掉
    model.userFactors.unpersist()
    model.itemFactors.unpersist()
  }

  def makeRecommendations(model: ALSModel, userId: Int, howMany: Int): DataFrame = {
    // 从模型中的艺术家特征矩阵中查出所有的艺术家的id，然后拼上userId
    val toRecommend = model.itemFactors.select($"id".as("artist")).withColumn("user", lit(userId))
    // 将上面得到的数据放到模型中进行计算，然后计算出按照推荐分数降序排，取指定的前几个
    model.transform(toRecommend).select("artist", "prediction").orderBy($"prediction".desc).limit(howMany)
  }


  def evaluate(rawUserArtistData: Dataset[String], rawArtistAlias: Dataset[String]): Unit = {
    val bArtistAlias = spark.sparkContext.broadcast(buildArtistAlias(rawArtistAlias))

    val allData = buildCounts(rawUserArtistData, bArtistAlias)
    // 将全部的数据集分成训练数据集和验证数据集
    val Array(trainData, validateData) = allData.randomSplit(Array(0.9, 0.1))
    trainData.cache()
    validateData.cache()
    // 拿到所有的艺术家的id，数据量不大，可以broadcast到每一个executor
    val allArtistIds = allData.select("artist").as[Int].distinct().collect()
    val bAllArtistIds = spark.sparkContext.broadcast(allArtistIds)


    // 利用训练数据集进行模型训练
    val model = new ALS().
      setSeed(Random.nextLong()). // 随机种子数
      setImplicitPrefs(true). // 我们这里的数据是隐式数据集
      setRank(10).  // 隐语义因子的个数，默认是10
      setRegParam(0.01). // 指定ALS的正则化参数，默认是1.0
      setAlpha(1.0).  //  控制矩阵分解时，被观察到的“用户- 产品”交互相对没被观察到的交互的权重
      setMaxIter(5). // 迭代的次数
      setUserCol("user"). // 用户列
      setItemCol("artist"). // 艺术家列
      setRatingCol("count"). // 播放次数
      setPredictionCol("prediction"). // 推荐值列
      fit(trainData) // 模型训练

    // 计算第一个模型的AUC(Area Under the Curve)
    val firstModelAUC = areaUnderCurve(validateData, bAllArtistIds, model.transform) //计算需要点时间
    println(firstModelAUC)
    //我们可以利用交叉验证的方法来求多个AUC，然后求其平均值，这个评估会更加的准确点

    // 选择合理的超参数(Hyperparameter)
    // 利用参数的组合来确定最佳的参数
    val evaluations =
      for (rank <- Seq(10, 50);
           regParam <- Seq(1.0, 0.0001);
           alpha <- Seq(1.0, 40.0)) yield {
        val model = new ALS().
          setSeed(Random.nextLong()).
          setImplicitPrefs(true).
          setRank(rank).setRegParam(regParam).
          setAlpha(alpha).setMaxIter(10).
          setUserCol("user").setItemCol("artist").
          setRatingCol("count").setPredictionCol("prediction").
          fit(trainData)

        val auc = areaUnderCurve(validateData, bAllArtistIds, model.transform)

        model.userFactors.unpersist()
        model.itemFactors.unpersist()

        (auc, (rank, regParam, alpha))
      }

    evaluations.sorted.reverse.foreach(println)

    trainData.unpersist()
    validateData.unpersist()
  }

  /**
    * 先计算每一个user的AUC(Area Under the Curve)，然后返回平均的AUC值
    * @param positiveData
    * @param bAllArtistIds
    * @param predictFunction
    * @return
    */
  def areaUnderCurve(positiveData: DataFrame,
                     bAllArtistIds: Broadcast[Array[Int]],
                     predictFunction: (DataFrame => DataFrame)): Double = {
    //  将拿出来的数据集作为"positive(积极的)"数据
    // 对积极的数据中的每一个user以及对应的artist计算推荐的分数
    val positivePredictions = predictFunction(positiveData.select("user", "artist")).
      withColumnRenamed("prediction", "positivePrediction")

    // 为每一个user创建一个"negative(消极的)"艺术家的集合
    // 这些艺术家是从所有的艺术家中随机抽取出来的，当然这些艺术家肯定不在positive数据集中
    val negativeData = positiveData.select("user", "artist").as[(Int, Int)].
      groupByKey { case (user, _) => user }.
      flatMapGroups { case (userId, userIdAndPosArtistIds) =>
        val posItemIdSet = userIdAndPosArtistIds.map(_._2).toSet //当前user对应的在positive data中所有的artist
        val negative = new ArrayBuffer[Int]() //用于存储当前user对应的negative artist id
        val allArtistIds = bAllArtistIds.value // 所有的artist id
        var i = 0
        val random = new Random()
        // 保证只遍历allArtistIds一遍，避免死循环
        // 还要保证negative的长度等于positive的长度
        while (i < allArtistIds.length && negative.size < posItemIdSet.size) {
          val artistId = allArtistIds(random.nextInt(allArtistIds.length)) // 从全部的艺术家数据中随机选择一个艺术家
          if (!posItemIdSet.contains(artistId)) { // 这个艺术家必须不在positive数据集中
            negative += artistId
          }
          i += 1
        }
        negative.map(artistId => (userId, artistId))
      }.toDF("user", "artist")

    // 对消极的数据中的每一个user以及对应的artist计算推荐的分数
    val negativePredictions = predictFunction(negativeData).
      withColumnRenamed("prediction", "negativePrediction")

    // 积极的预测的数据和消极的预测数据进行关联
    val joinedPredictions = positivePredictions.join(negativePredictions, "user")
      .select("user", "positivePrediction", "negativePrediction").cache()
    // 对关联之后的数据按照user进行分组求总和
    val allCounts = joinedPredictions.groupBy("user").agg(count(lit(1)).as("total")).select("user", "total")
    // 计算每一个user的positive预测分大于negative预测分的总数
    val correctCounts = joinedPredictions.filter($"positivePrediction" > $"negativePrediction")
      .groupBy("user").agg(count("user").as("correct")).
      select("user", "correct")
    // 计算每一个用户的AUC，然后对所有user的AUC进行平均
    val meanAUC = allCounts.join(correctCounts, Seq("user"), "left_outer")
      .select($"user", (coalesce($"correct", lit(0)) / $"total").as("auc")).
      agg(mean("auc")).as[Double].first()

    joinedPredictions.unpersist()
    meanAUC //1:说明模型质量非常好，完美， 0.5： 质量一般    0：质量非常差劲
  }


  /**
    *  使用最优的参数来建模进行推荐
    * @param rawUserArtistData
    * @param rawArtistData
    * @param rawArtistAlias
    */
  def recommend(rawUserArtistData: Dataset[String],
                rawArtistData: Dataset[String],
                rawArtistAlias: Dataset[String]): Unit = {

    val bArtistAlias = spark.sparkContext.broadcast(buildArtistAlias(rawArtistAlias))
    val allData = buildCounts(rawUserArtistData, bArtistAlias).cache()
    val model = new ALS().
      setSeed(Random.nextLong()).
      setImplicitPrefs(true).
      setRank(10).setRegParam(1.0).setAlpha(40.0).setMaxIter(5).
      setUserCol("user").setItemCol("artist").
      setRatingCol("count").setPredictionCol("prediction").
      fit(allData)
    allData.unpersist()

    val userID = 2093760
    val topRecommendations = makeRecommendations(model, userID, 5)

    val recommendedArtistIDs = topRecommendations.select("artist").as[Int].collect()
    val artistByID = buildArtistById(rawArtistData)
    artistByID.join(spark.createDataset(recommendedArtistIDs).toDF("id"), "id").
      select("name").show()

    model.userFactors.unpersist()
    model.itemFactors.unpersist()
  }
}
