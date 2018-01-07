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

case class ImageData(base64: String) extends HasBase64String

case class ImageDataWithTimestamp(base64: String, timestamp: Long) extends HasBase64String with HasTimestampLong

case class ImageDataDB(_id: ObjectId, base64: String, timestamp: BsonDateTime) 
  extends HasBase64String with HasTimestampBson with HasObjectId

object ImageDataDB {
  def apply(base64: String, timestamp: Long): ImageDataDB =
    ImageDataDB(new ObjectId(), base64, BsonDateTime(timestamp))
    
  def apply(imageDataWithTimestamp: ImageDataWithTimestamp): ImageDataDB =
    ImageDataDB(new ObjectId(), imageDataWithTimestamp.base64, BsonDateTime(imageDataWithTimestamp.timestamp))
    
  def toImageDataWithTimestamp(imageDataDB: ImageDataDB): ImageDataWithTimestamp = 
    ImageDataWithTimestamp(imageDataDB.base64, imageDataDB.timestamp.getValue)
}
