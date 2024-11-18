
import paho.mqtt.client as mqtt
import random
import time
import json

# Flag to track connection status
is_connected = False

def on_connect(client, userdata, flags, rc):
    global is_connected
    if rc == 0:
        print("Connected successfully")
        is_connected = True
    else:
        print(f"Connect failed with code {rc}")

def on_publish(client, userdata, mid):
    print(f"Published message with mid: {mid}")

def on_disconnect(client, userdata, rc):
    global is_connected
    print("Disconnected with result code: %s", rc)
    is_connected = False

# Create client
client_id = f'publisher-{random.randint(0, 100)}'
mqtt_client = mqtt.Client(client_id=client_id)

# Set callbacks
mqtt_client.on_connect = on_connect
mqtt_client.on_publish = on_publish
mqtt_client.on_disconnect = on_disconnect

# Connect to broker
mqtt_client.connect('broker.hivemq.com', 1883)
mqtt_client.loop_start()

# Wait for connection
time.sleep(2)  # Give time for connection to establish

try:
    while True:
        if is_connected:
            data = {
                'Age': 68,
                'Sex': 'M',
                'ChestPainType': 'TA',
                'RestingBP': 160,
                'Cholesterol': 320,
                'FastingBS': 1,
                'RestingECG': 'ST',
                'MaxHR': 120,
                'ExerciseAngina': 'Y',
                'Oldpeak': 2.5,
                'ST_Slope': 'Down',
                'DEVICE_ID': 'd_FKbhzHRHaqKkDCIVG_Hp:APA91bHO1CtEPifDj5FkwJJxmPmuBW_FMjcxrRK6nhtnNu7c9BjNFol_BErs7kBt9Xk47JgTTkm76oBvT-KC3tj1ZnjzLStjDDnt0yp5kzCsRpawJ7yZIIw'
            }
            
            info = mqtt_client.publish(topic='watch/heartbeat', 
                                     payload=json.dumps(data), 
                                     qos=1)  # Changed QoS to 1 for better reliability
            
            info.wait_for_publish()
            print(f"Published: {info.is_published()}")
            time.sleep(1)
        else:
            print("Waiting for connection...")
            time.sleep(1)

except KeyboardInterrupt:
    print("Stopping...")
    mqtt_client.loop_stop()
    mqtt_client.disconnect()
