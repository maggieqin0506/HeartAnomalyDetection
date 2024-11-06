from paho.mqtt import client as mqtt_client
import random 

port = 1883
topic = "watch/heartbeat"
client_id = f'subscribe-{random.randint(0, 100)}'

# kafka
from confluent_kafka import Producer

def connect_mqtt() -> mqtt_client:
    def on_connect(client, userdata, flags, rc):
        if rc == 0:
            print("Connected to MQTT Broker!")
        else:
            print("Failed to connect, return code %d\n", rc)

    def on_log(client, userdata, level, buf):
        print("log:", buf)
        
    client = mqtt_client.Client()
    client.on_connect = on_connect

    client.on_log = on_log
    client.connect("mqtt5", port, 60)
    return client

def subscribe(client: mqtt_client, kafka_producer: Producer):
    def on_message(client, userdata, msg):
        print(f"Received `{msg.payload.decode()}` from `{msg.topic}` topic")
        # put data sent from "watch" to kafka's topic
        kafka_producer.produce(topic='heartbeat', key='device_id', value=msg.payload.decode(), callback=delivery_callback)
        kafka_producer.poll(0)
        kafka_producer.flush()
    client.subscribe(topic)
    client.on_message = on_message

def delivery_callback(err, msg):
        if err:
            print('ERROR: Message failed delivery: {}'.format(err))
        else:
            print("Produced event to topic {topic}: key = {key:12} value = {value:12}".format(
                topic=msg.topic(), key=msg.key().decode('utf-8'), value=msg.value().decode('utf-8')))

def connect_kafka_producer():
    try:
        p = Producer({'bootstrap.servers': 'kafka:29092'})
        print("Connected to Kafka Broker!")
        return p
    except Exception as e:
        print(f"Failed to connect to Kafka Broker: {e}")
        return None


def run():
    client = connect_mqtt()
    kafka_producer = connect_kafka_producer()
    subscribe(client, kafka_producer)
    client.loop_forever()

if __name__ == '__main__':
    run()