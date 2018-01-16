import cv2
from time import sleep
import base64
import requests
import json

def convertImageToBase64():
  with open("image_test.jpg", "rb") as image_file:
    encoded = base64.b64encode(image_file.read())
    return encoded

cap = cv2.VideoCapture(0)
cap.set(3, 640)
cap.set(4, 480)

pictures = 0

while True:
  sleep(10)
  pictures = pictures + 1
  print('Taking picture nr ' + str(pictures))
  ret, frame = cap.read()
  cv2.imwrite('image.jpg',frame)
  
  image_64 = base64.b64encode(open("image.jpg","rb").read())
  
  payload = json.dumps({'base64' : image_64 })
  
  headers = {'Content-Type' : 'application/json'}
  r = requests.post("http://192.168.1.254:8080/TestOrigin/image/insert", headers = headers, data = payload)
  
  print(r)
