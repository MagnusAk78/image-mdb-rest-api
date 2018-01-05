package model

import org.mongodb.scala.bson.ObjectId

case class ImageDataWithTimestamp(
  // base64 string
  base64: String,

  // Timestamp
  timestamp: Long) {
}