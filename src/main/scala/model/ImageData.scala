package model

import org.mongodb.scala.bson.ObjectId

case class ImageData(
  // base64 string that is the acutal image
  base64: String,

  // Timestamp
  timestamp: Long) {
}