package model

import org.mongodb.scala.bson.ObjectId

object ImageDataDB {
  def apply(base64: String, timestamp: Long): ImageDataDB =
    ImageDataDB(new ObjectId(), base64, timestamp)
    
  def toImageData(imageDataDB: ImageDataDB) = ImageData(imageDataDB.base64, imageDataDB.timestamp)
}

case class ImageDataDB(
  // Required object ID
  _id: ObjectId,
  
  // base64 string that is the acutal image
  base64: String,

  // Timestamp
  timestamp: Long) {
}