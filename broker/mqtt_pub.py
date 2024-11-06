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
while True:
    data = {
        'Age': 60,
        'Sex': 'M', 
        'ChestPainType': 'ATA',
        'RestingBP': 130,
        'Cholesterol': 250, 
        'FastingBS': 0,
        'RestingECG': 'Normal',
        'MaxHR': 150,
        'ExerciseAngina': 'N',
        'Oldpeak': 1.0,
        'ST_Slope': 'Up'
    }
    info = mqtt_client.publish(topic='watch/heartbeat', payload=json.dumps(data), qos=0)

    info.wait_for_publish()
    print(info.is_published())
    time.sleep(1)