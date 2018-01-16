import picamera
from time import sleep
import base64
import requests
import json

def convertImageToBase64():
  with open("image_test.jpg", "rb") as image_file:
    encoded = base64.b64encode(image_file.read())
    return encoded

camera = picamera.PiCamera()
camera.resolution = (640,480)

while True:
  sleep(5)
  camera.capture('image.jpg')
  
  image_64 = base64.b64encode(open("image.jpg","rb").read())
  
  payload = json.dumps({'base64' : image_64 })
  
  headers = {'Content-Type' : 'application/json'}
  r = requests.post("http://192.168.1.254:8080/TestOrigin/image/insert", headers = headers, data = payload)
