from pyspark.sql import SparkSession
from pyspark.sql.types import StructType, StructField, StringType, IntegerType, FloatType
from pyspark.sql.functions import from_json
import joblib
import pickle

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

model = joblib.load('/opt/heart.model')
label_encoders = pickle.load(open('/opt/label_encoders.pkl', 'rb'))

# Function to apply scikit-learn model to each micro-batch
def process_batch(batch_df, batch_id):
    # Convert the Spark DataFrame to Pandas DataFrame
    pandas_df = batch_df.toPandas()
    pandas_df = pandas_df.dropna(how='any')
    
    # Use your pre-trained model to make predictions
    for column in ['Sex', 'ChestPainType', 'RestingECG', 'ExerciseAngina', 'ST_Slope']:
        pandas_df[column] = label_encoders[column].transform(pandas_df[column])
    if len(pandas_df) > 0:
        predictions = model.predict(pandas_df)

    # Add predictions to the DataFrame
        pandas_df['predictions'] = predictions

    # Optionally, perform further actions, e.g., save to database, push to another Kafka topic, etc.
    # For now, let's just print the predictions
        print('Res', pandas_df)

# Write the stream and apply the model using foreachBatch
query = kafka_values.writeStream \
    .outputMode("append") \
    .foreachBatch(process_batch) \
    .trigger(processingTime="1 second") \
    .option("checkpointLocation", "checkpoint") \
    .start()

query.awaitTermination()