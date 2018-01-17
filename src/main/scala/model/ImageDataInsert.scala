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

case class ImageDataInsert(base64: String) extends HasBase64String 

case class ImageDataPresented(originName: String, base64: String, timestamp: Long) extends HasOriginName 
  with HasBase64String with HasTimestampLong

case class ImageDataDB(_id: ObjectId, originName: String, base64: String, timestamp: BsonDateTime) 
  extends HasObjectId with HasOriginName with HasBase64String with HasTimestampBson 

object ImageDataDB {
  def apply(originName: String, base64: String, timestamp: Long): ImageDataDB =
    ImageDataDB(new ObjectId(), originName, base64, BsonDateTime(timestamp))
    
  def apply(imageDataPresented: ImageDataPresented): ImageDataDB =
    ImageDataDB(new ObjectId(), imageDataPresented.originName, imageDataPresented.base64, 
        BsonDateTime(imageDataPresented.timestamp))
    
  def toImageDataPresented(imageDataDB: ImageDataDB): ImageDataPresented = 
    ImageDataPresented(imageDataDB.originName, imageDataDB.base64, imageDataDB.timestamp.getValue)
}
