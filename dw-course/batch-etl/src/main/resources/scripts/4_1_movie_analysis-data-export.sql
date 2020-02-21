-- 1、一部电影或者好几部电影的平均评分是多少？
CREATE TABLE avg_movie_rating AS
SELECT movie_id,ROUND(AVG(rating),1) as rating
FROM user_rating_fact
GROUP BY movie_id;

-- 2、在mysql中创建表
CREATE TABLE avg_movie_rating(movie_id INT, rating DOUBLE);

sqoop export --connect jdbc:mysql://master:3306/movie \
--username root --password WESTAR@soft1 \
--table avg_movie_rating \
--export-dir /user/hive/warehouse/movielens.db/avg_movie_rating \
--m 2 \
--update-key movie_id \
--update-mode allowinsert \
--input-fields-terminated-by '\001' --lines-terminated-by '\n'

