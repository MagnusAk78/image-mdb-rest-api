package model

import org.mongodb.scala.bson.ObjectId

object ImageDataDB {
  def apply(base64: String, timestamp: Long): ImageDataDB =
    ImageDataDB(new ObjectId(), base64, timestamp)
    
  def apply(imageDataWithTimestamp: ImageDataWithTimestamp): ImageDataDB =
    ImageDataDB(new ObjectId(), imageDataWithTimestamp.base64, imageDataWithTimestamp.timestamp)    
    
  def toImageDataWithTimestamp(imageDataDB: ImageDataDB): ImageDataWithTimestamp = 
    ImageDataWithTimestamp(imageDataDB.base64, imageDataDB.timestamp)
}

case class ImageDataDB(
  // Required object ID
  _id: ObjectId,
  
  // base64 string
  base64: String,

  // Timestamp
  timestamp: Long) {
}