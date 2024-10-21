from pyspark.sql import SparkSession
import sys
import os

os.environ['HADOOP_HOME'] = "C:/Program Files/hadoop"
sys.path.append("C:/Program Files/hadoop/bin")

spark = SparkSession.builder \
    .appName("KafkaStreamProcessing") \
    .config("spark.jars.packages", "org.apache.spark:spark-sql-kafka-0-10_2.12:3.5.3") \
    .config("spark.driver.extraJavaOptions", "-Djava.security.manager=allow") \
    .config("spark.executor.extraJavaOptions", "-Djava.security.manager=allow").getOrCreate()

kafka_df = spark.readStream.format("kafka") \
.option("kafka.bootstrap.servers", "localhost:9092") \
.option("subscribe", "heartbeat") \
.load()

kafka_values = kafka_df.selectExpr("CAST(value AS STRING)")

# Process the data
query = kafka_values.writeStream \
  .outputMode("append") \
  .format("console") \
  .start()

query.awaitTermination()

#!/usr/bin/env python

# from confluent_kafka import Consumer

# if __name__ == '__main__':

#     config = {
#         # User-specific properties that you must set
#         'bootstrap.servers': 'localhost:9092',

#         # Fixed properties
#         'group.id':          'kafka-python-getting-started',
#         'auto.offset.reset': 'earliest'
#     }

#     # Create Consumer instance
#     consumer = Consumer(config)

#     # Subscribe to topic
#     topic = "heartbeat"
#     consumer.subscribe([topic])

#     # Poll for new messages from Kafka and print them.
#     try:
#         while True:
#             msg = consumer.poll(1.0)
#             if msg is None:
#                 continue
#             elif msg.error():
#                 print("ERROR: %s".format(msg.error()))
#             else:
#                 # Extract the (optional) key and value, and print.
#                 print("Consumed event from topic {topic}: key = {key:12} value = {value:12}".format(
#                     topic=msg.topic(), key=msg.key().decode('utf-8'), value=msg.value().decode('utf-8')))
#     except KeyboardInterrupt:
#         pass
#     finally:
#         # Leave group and commit final offsets
#         consumer.close()