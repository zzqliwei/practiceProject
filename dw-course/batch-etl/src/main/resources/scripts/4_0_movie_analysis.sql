-- 1、一部电影或者好几部电影的平均评分是多少？
SELECT movie_id,ROUND(AVG(rating),1) as rating
FROM user_rating_fact
GROUP BY movie_id;

--2、本周的趋势是什么样的？也就是说从本周开始哪些电影的评分有增长
WITH currentWeekMovieRating as(
 SELECT movie_id,max(rating) as rating FROM user_rating
 WHERE year(dt_time) = year(current_date) AND weekofyear(dt_time)=weekofyear(current_date)
 GROUP BY movie_id),
 previousWeekMovieRating as (
 SELECT movie_id,max(rating) as rating FROM user_rating
 WHERE year(dt_time) != year(current_date) AND weekofyear(dt_time)!=weekofyear(current_date)
 GROUP BY movie_id
)
SELECT t1.movie_id FROM currentWeekMovieRating t1
LEFT JOIN previousWeekMovieRating t2 on t1.movie_id = t2.movie_id
WHERE (t1-COALESCE(t2.rating,0))>0;

--COALESCE 这个参数使用的场合为：假如某个字段默认是null，
--你想其返回的不是null，而是比如0或其他值，可以使用这个函数

-- 3、给电影Dead Man Walking(1995)评分在3分或者3分以上的用户中，21岁到30岁的女性用户占多少比例
--t1 查询满足 名称 条件的电影ID
--t2 查询满足 评分 条件的电影用户性别，年龄
--t3 统计满足t1 和 t2 的个数
WITH t1 as ( SELECT id FROM movie WHERE title="Dead Man Walking(1995)" ),
     t2 as (
         SELECT uses.gender as gender,users.age as age
         FROM user_rating_fact
         JOIN t1 ON user_rating_fact.movie_id=t1.id
         JOIN users on user_rating_fact.user_id=users.id
         WHERE user_rating_fact.rating >= 3),
     t3 as (select count(*) as cnt from t2),
     t4 as (
         SELECT count(*) as cnt FROM t2
         WHERE t2.gender='F'
         AND t2.age BETWEEN 21 AND 30)
SELECT round(t4.cnt / t3.cnt,2) from t3,t4;

-- 4、在三个月内更新了同一部电影的评分的用户数的占比是多少？
set hive.strict.checks.cartesian.product=false;
--t1 人员更新了同一部电影评价
-- t2 人员总数
WITH t1 as (
    SELECT user_id,movie_id
    FROM user_rating
    WHERE dt_time BETWEEN ADD_MONTHS(current_date,-3) AND CURRENT_DATE
    GROUP BY user_id,movie_id
    HAVING count(*)>1 ),
t2 as ( SELECT count(*) as allCnt FROM users),
t3 as (SELECT count(DISTINCT movie)  as cnt from t1)
select round(t3/t2,2) from t2,t3