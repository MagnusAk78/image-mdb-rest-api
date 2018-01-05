package control

import akka.actor.{ Actor, ActorRef, Props }
import model.ImageDataDB
import model.ImagesInfo
import model.ErrorMessage

case class AskImageTimestampMessage(timestamp: Long)
case object AskImageLastMessage

case object AskImagesCountMessage
case object AskImagesListMessage

class MongoDBClient() extends Actor with akka.actor.ActorLogging {

  import org.mongodb.scala._
  
  import org.mongodb.scala.model.Filters._
  import org.mongodb.scala.bson.codecs.Macros._
  import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
  import org.bson.codecs.configuration.CodecRegistries.{fromRegistries, fromProviders}
    
  // Use a Connection String
  val mongoClient: MongoClient = MongoClient("mongodb://localhost")
  
  val ImageDataDBCodecRegistry = fromRegistries(fromProviders(classOf[ImageDataDB]), DEFAULT_CODEC_REGISTRY )

  val database: MongoDatabase = mongoClient.getDatabase("images").withCodecRegistry(ImageDataDBCodecRegistry)
  
  val collection: MongoCollection[ImageDataDB] = database.getCollection("images")

  def receive = {

    case AskImageTimestampMessage(timestamp) ⇒ {
      log.info("MongoDBClient - AskImageListMessage")
      
      collection.find(equal("timestamp", timestamp)).first().subscribe(new Observer[ImageDataDB] {

        override def onNext(imageDataDB: ImageDataDB): Unit = {
          sender ! Left(ImageDataDB.toImageData(imageDataDB))
        }

        override def onError(e: Throwable): Unit = {
          sender ! Right(ErrorMessage("Error finding image data"))
        }

        override def onComplete(): Unit = log.info("MongoDBClient - AskImageLastMessage End")
      })
      
      sender ! ImageDataDB.apply(base64 = "test", timestamp = System.currentTimeMillis)
    }
    
    case AskImageLastMessage ⇒ {
      log.info("MongoDBClient - AskImageLastMessage Start")
      
      collection.find.first().subscribe(new Observer[ImageDataDB] {

        override def onNext(imageDataDB: ImageDataDB): Unit = {
          sender ! Left(ImageDataDB.toImageData(imageDataDB))
        }

        override def onError(e: Throwable): Unit = {
          sender ! Right(ErrorMessage("Error finding image data"))
        }

        override def onComplete(): Unit = log.info("MongoDBClient - AskImageLastMessage End")
      })
    }
    
    case AskImagesCountMessage ⇒ {
      log.info("MongoDBClient - AskImagesCountMessage Start")
      
      collection.count().subscribe(new Observer[Long] {

        override def onNext(countValue: Long): Unit = {
          sender ! ImagesInfo(count = countValue)
        }

        override def onError(e: Throwable): Unit = {
          sender ! ImagesInfo(count = 0)
        }

        override def onComplete(): Unit = log.info("MongoDBClient - AskImagesCountMessage End")
      })
    }
    
    case AskImagesListMessage ⇒ {
      log.info("MongoDBClient - AskImagesListMessage")
      
      val currentTimestamp = System.currentTimeMillis
      sender ! List(currentTimestamp - 3000, currentTimestamp - 2000, currentTimestamp - 1000)
    }    
  }
}