from pyspark.sql import SparkSession
from pyspark.sql.types import StructType, StructField, StringType, IntegerType, FloatType
from pyspark.sql.functions import from_json, udf, struct

data_schema = StructType([
    StructField("Age", IntegerType(), True),
    StructField("Sex", StringType(), True),
    StructField("ChestPainType", StringType(), True),
    StructField("RestingBP", IntegerType(), True),
    StructField("Cholesterol", IntegerType(), True),
    StructField("FastingBS", IntegerType(), True),
    StructField("RestingECG", StringType(), True),
    StructField("MaxHR", IntegerType(), True),
    StructField("ExerciseAngina", StringType(), True),
    StructField("Oldpeak", FloatType(), True),
    StructField("ST_Slope", StringType(), True)
])

spark = SparkSession.builder \
    .appName("KafkaStreamProcessing") \
    .getOrCreate()

kafka_df = spark.readStream.format("kafka") \
    .option("startingOffsets", "earliest") \
    .option("kafka.bootstrap.servers", "kafka:29092") \
    .option("subscribe", "heartbeat") \
    .option("kafka.group.id", "spark_cluster") \
    .load()

kafka_values = kafka_df.selectExpr("CAST(value AS STRING) AS json_value") \
    .select(from_json("json_value", data_schema).alias("data")) \
    .select("data.*")



# Process the data with checkpointing
query = kafka_values.writeStream \
  .outputMode("append") \
  .format("console") \
  .option("checkpointLocation", "checkpoint") \
  .start()

query.awaitTermination()