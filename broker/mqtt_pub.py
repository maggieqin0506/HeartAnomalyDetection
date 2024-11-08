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
        'Age': 68,              # Older age
        'Sex': 'M',             
        'ChestPainType': 'TA',  # Typical Angina (associated with heart disease)
        'RestingBP': 160,       # High resting blood pressure
        'Cholesterol': 320,     # High cholesterol level
        'FastingBS': 1,         # High fasting blood sugar
        'RestingECG': 'ST',     # Abnormal ECG
        'MaxHR': 120,           # Lower than normal maximum heart rate
        'ExerciseAngina': 'Y',  # Exercise-induced angina
        'Oldpeak': 2.5,         # Higher ST depression
        'ST_Slope': 'Down',    # Downsloping ST segment,
        'DEVICE_ID': 'device_1'
}
    info = mqtt_client.publish(topic='watch/heartbeat', payload=json.dumps(data), qos=0)

    info.wait_for_publish()
    print(info.is_published())
    time.sleep(1)