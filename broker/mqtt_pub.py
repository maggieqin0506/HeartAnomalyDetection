import paho.mqtt.client as mqtt
import random
import time
import json


def on_publish(client, userdata, mid):
    print(f"Published message with mid: {mid}")

mqtt_client = mqtt.Client()
mqtt_client.on_publish = on_publish
mqtt_client.connect('localhost', 1883)

mqtt_client.loop_start()
i = 0
while True:
    data = {
        "heart_rate": 60,
        "spo2": 98,
        "temp": 36.5,
        "timestamp": "2021-07-15 12:00:00",
        "device_id": i
    }
    info = mqtt_client.publish(topic='watch/heartbeat', payload=json.dumps(data), qos=0)
    i += 1
    info.wait_for_publish()
    print(info.is_published())
    time.sleep(1)