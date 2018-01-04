package control

import akka.actor.{ Actor, ActorRef, Props }
import model.ImageData
import model.ImagesInfo

case class AskImageTimestampMessage(timestamp: Long)
case object AskImageLastMessage

case object AskImagesCountMessage
case object AskImagesListMessage

class MongoDBClient() extends Actor with akka.actor.ActorLogging {

  import org.mongodb.scala._
    
  // Use a Connection String
  val mongoClient: MongoClient = MongoClient("mongodb://localhost")

  val database: MongoDatabase = mongoClient.getDatabase("images")

  def receive = {

    case AskImageTimestampMessage(timestamp) ⇒ {
      log.info("MongoDBClient - AskImageListMessage")
      
      sender ! new ImageData(base64 = "test", timestamp = System.currentTimeMillis)
    }
    
    case AskImageLastMessage ⇒ {
      log.info("MongoDBClient - AskImageLastMessage")
      
      sender ! new ImageData(base64 = "test", timestamp = System.currentTimeMillis)
    }
    
    case AskImagesCountMessage ⇒ {
      log.info("MongoDBClient - AskImagesCountMessage")
      
      sender ! new ImagesInfo(count = 45)
    }
    
    case AskImagesListMessage ⇒ {
      log.info("MongoDBClient - AskImagesListMessage")
      
      val currentTimestamp = System.currentTimeMillis
      sender ! List(currentTimestamp - 3000, currentTimestamp - 2000, currentTimestamp - 1000)
    }    
  }
}