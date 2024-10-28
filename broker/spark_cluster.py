from pyspark.sql import SparkSession

spark = SparkSession.builder \
    .appName("KafkaStreamProcessing") \
    .config("spark.jars.packages", "org.apache.spark:spark-sql-kafka-0-10_2.12:3.5.3") \
    .config("spark.driver.extraJavaOptions", "-Djava.security.manager=allow") \
    .config("spark.executor.extraJavaOptions", "-Djava.security.manager=allow") \
    .getOrCreate()

kafka_df = spark.readStream.format("kafka") \
    .option("startingOffsets", "earliest") \
    .option("kafka.bootstrap.servers", "kafka:29092") \
    .option("subscribe", "heartbeat") \
    .option("kafka.group.id", "spark_cluster") \
    .load()

kafka_values = kafka_df.selectExpr("CAST(value AS STRING)")

# Process the data with checkpointing
query = kafka_values.writeStream \
  .outputMode("append") \
  .format("console") \
  .option("checkpointLocation", "/tmp/checkpoint") \
  .start()

query.awaitTermination()