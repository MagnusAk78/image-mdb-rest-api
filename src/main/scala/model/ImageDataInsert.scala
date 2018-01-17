package model

import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.bson.BsonDateTime
import org.mongodb.scala.bson.BsonBinary

import util.Gzip

trait HasBase64String {
  // base64 string
  val base64: String
}

trait HasGzipBase64String {
  // base64 string
  val gzipBase64: String
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
  
case class ImageDataPresentedGzip(originName: String, gzipBase64: String, timestamp: Long) extends HasOriginName 
  with HasGzipBase64String with HasTimestampLong

case class ImageDataDB(_id: ObjectId, originName: String, base64: String, timestamp: BsonDateTime, 
    gzipBase64: String) extends HasObjectId with HasOriginName with HasBase64String with HasTimestampBson 
      with HasGzipBase64String

object ImageDataDB {
  def apply(originName: String, base64: String, timestamp: Long): ImageDataDB =
    ImageDataDB(new ObjectId(), originName, base64, BsonDateTime(timestamp), Gzip.compress(base64))
    
  def apply(imageDataPresented: ImageDataPresented): ImageDataDB =
    ImageDataDB(new ObjectId(), imageDataPresented.originName, imageDataPresented.base64, 
        BsonDateTime(imageDataPresented.timestamp), Gzip.compress(imageDataPresented.base64))
        
  def apply(imageDataPresentedGzip: ImageDataPresentedGzip): ImageDataDB =
    ImageDataDB(new ObjectId(), imageDataPresentedGzip.originName, Gzip.decompress(imageDataPresentedGzip.gzipBase64), 
        BsonDateTime(imageDataPresentedGzip.timestamp), imageDataPresentedGzip.gzipBase64)        
    
  def toImageDataPresented(imageDataDB: ImageDataDB): ImageDataPresented = {
    BsonBinary
    ImageDataPresented(imageDataDB.originName, imageDataDB.base64, imageDataDB.timestamp.getValue)
  }
}
