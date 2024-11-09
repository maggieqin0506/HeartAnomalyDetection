import firebase_admin
from fastapi import FastAPI
from firebase_admin import credentials
from firebase_admin import messaging
from pydantic import BaseModel

app = FastAPI()
class WatchData(BaseModel):
    Age: int
    Sex: str 
    ChestPainType: str
    RestingBP: int
    Cholesterol: int
    FastingBS: int
    RestingECG: str
    MaxHR: int
    ExerciseAngina: str
    Oldpeak: float
    ST_Slope: str
    DEVICE_ID: str
    prediction: int


server_key = '/code/app/cmpe272-cardio-alert-firebase-adminsdk-tjjgj-d1c198320f.json'
cred = credentials.Certificate(server_key)
firebase_admin.initialize_app(cred)

@app.get("/")
async def root():
    return {"message": "Hello World"}

@app.post("/send-notification/")
def send_notification(watch_data: WatchData):
    print(watch_data)
    # Replace with the path to your service account key JSON file

    # Replace with your device tokens
    device_tokens = watch_data.DEVICE_ID


    print(device_tokens)

    # Notification details
    title = 'Heart rate alert'
    body = 'Emergency!!!'
    data = watch_data.model_dump()
    if data:
        data = {k: str(v) for k, v in data.items()}

    # Send push notification
    send_push_notification(device_tokens, title, body, data)


def send_push_notification(device_tokens, title, body, data=None):
    message = messaging.Message(
        notification=messaging.Notification(
            title=title,
            body=body,
        ),
        data=data,
        token=device_tokens,
    )
    response = messaging.send(message)
    # Response is a message ID string.
    print("Successfully sent notification:", response)


