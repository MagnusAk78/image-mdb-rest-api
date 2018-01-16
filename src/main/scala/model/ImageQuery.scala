package model

import org.mongodb.scala.bson.ObjectId

case class ImageQuery(
    fromTimestamp: Long, 
    toTimestamp: Long,
    limit: Int)