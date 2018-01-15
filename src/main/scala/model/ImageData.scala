package model

import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.bson.BsonDateTime

trait HasBase64String {
  // base64 string
  val base64: String
}

trait HasTimestampBson {
  // Timestamp
  val timestamp: BsonDateTime
}

trait HasTimestampLong {
  // Timestamp
  val timestamp: Long
}

trait HasObjectId {
  // Object ID
  val _id: ObjectId
}

trait HasOriginName {
  // Name of origin
  val originName: String
}

case class ImageData(originName: String, base64: String) extends HasOriginName with HasBase64String 

case class ImageDataWithTimestamp(originName: String, base64: String, timestamp: Long) extends HasOriginName 
  with HasBase64String with HasTimestampLong

case class ImageDataDB(_id: ObjectId, originName: String, base64: String, timestamp: BsonDateTime) 
  extends HasObjectId with HasOriginName with HasBase64String with HasTimestampBson 

object ImageDataDB {
  def apply(originName: String, base64: String, timestamp: Long): ImageDataDB =
    ImageDataDB(new ObjectId(), originName, base64, BsonDateTime(timestamp))
    
  def apply(imageDataWithTimestamp: ImageDataWithTimestamp): ImageDataDB =
    ImageDataDB(new ObjectId(), imageDataWithTimestamp.originName, imageDataWithTimestamp.base64, 
        BsonDateTime(imageDataWithTimestamp.timestamp))
    
  def toImageDataWithTimestamp(imageDataDB: ImageDataDB): ImageDataWithTimestamp = 
    ImageDataWithTimestamp(imageDataDB.originName, imageDataDB.base64, imageDataDB.timestamp.getValue)
}
