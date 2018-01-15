package model

import org.mongodb.scala.bson.ObjectId

case class ImageQuery(
    originName: String,
    fromTimestamp: Long, 
    toTimestamp: Long,
    limit: Int) extends HasOriginName