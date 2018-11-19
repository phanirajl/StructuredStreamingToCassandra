import com.holdenkarau.spark.testing.DataFrameSuiteBase
import org.apache.spark.sql.functions._
import org.scalatest.FunSuite

class Test extends FunSuite with DataFrameSuiteBase {

  test("Testing count of bots in streaming data") {

    val df = spark.read
      .json("/Users/mnetreba/Downloads/data.json")
      .na.drop()
      .toDF("category","ip","type","time")
      .groupBy("ip")
      .agg(min(from_unixtime(col("time"),"yyyy-MM-dd HH:mm:ss")) as "time_start",
        max(from_unixtime(col("time"),"yyyy-MM-dd HH:mm:ss")) as "time_end",
        collect_set("category") as "categories",
        count(when(col("type")==="click", 1)) as "clicks",
        count(when(col("type")==="view", 1)) as "views",
        count("type") as "requests")
      .withColumn("distinct_categories",size(col("categories")))
      .withColumn("duration_minutes",from_unixtime(unix_timestamp(col("time_end")).minus(unix_timestamp(col("time_start"))),"mm"))
      .withColumn("event_rate",col("requests").divide(col("duration_minutes")))
      .withColumn("categories_rate",col("distinct_categories").divide(col("duration_minutes")))
      .withColumn("views_clicks", when(col("views")>0, col("clicks").divide(col("views"))).otherwise(col("clicks")))
      .withColumn("bot", when(col("views_clicks")>3 or col("categories_rate")>0.5 or col("event_rate")>100,"yes").otherwise("no"))
      .select("ip","bot","duration_minutes","distinct_categories","event_rate","categories_rate","views_clicks")

    val bots = df.select(col("*")).where("bot == 'yes'")

    bots.show()

    assert(bots.count(), 1)
  }

}
