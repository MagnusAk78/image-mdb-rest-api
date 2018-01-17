package control

import org.bson.codecs.configuration.CodecRegistries.fromProviders
import org.bson.codecs.configuration.CodecRegistries.fromRegistries
import org.mongodb.scala.MongoClient
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.MongoDatabase
import org.mongodb.scala.Observer
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros.createCodecProvider
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Sorts._
import org.mongodb.scala.model.Projections._
import org.mongodb.scala.model.Indexes
import org.mongodb.scala.model.IndexModel
import org.mongodb.scala.model.IndexOptions
import org.mongodb.scala.Document
import rest.SettingsImpl

import java.util.concurrent.TimeUnit
import org.mongodb.scala.bson.BsonDateTime

import akka.actor.Actor
import akka.actor.actorRef2Scala
import model._

case class AskTimestampImageMessage(originName: String, timestamp: Long)
case class AskLatestImageMessage(originName: String)
case class AskOldestImageMessage(originName: String)
case class InsertImageMessage(originName: String, imageData: ImageDataInsert)
case class InsertImageDataPresentedMessage(imageDataPresented: ImageDataPresented)
case class InsertImageDataPresentedGzipMessage(imageDataPresentedGzip: ImageDataPresentedGzip)

case class AskImagesCountMessage(originName: String)
case class AskQueryImagesListMessage(originName: String, imageQuery: ImageQuery)

case class ResponseCreateIndex(resultOk: Boolean, errorMessage: Option[String])
case class ResponseDropIndex(resultOk: Boolean, errorMessage: Option[String])

class MongoDBClient(settings: SettingsImpl) extends Actor with akka.actor.ActorLogging {

  // Use a Connection String
  val mongoClient: MongoClient = MongoClient("mongodb://" + settings.MongoDbIpAddress + ":" + settings.MongoDbPort)

  val ImageDataDBCodecRegistry = fromRegistries(fromProviders(classOf[ImageDataDB]), DEFAULT_CODEC_REGISTRY)

  val database: MongoDatabase = mongoClient.getDatabase(settings.MongoDbDatabaseName).withCodecRegistry(ImageDataDBCodecRegistry)

  val caseClassCollection: MongoCollection[ImageDataDB] = database.getCollection(settings.MongoDbCollectionName)

  val documentCollection: MongoCollection[Document] = database.getCollection(settings.MongoDbCollectionName)

  log.info("MongoDBClient - Dropping potential index (in case we have changed TTL)...")
  // Set 'timestamp' as a descending index and set expire time according to settings value
  documentCollection.dropIndex(Indexes.descending("timestamp")).subscribe(
    new ObserveDropIndex(this.context.self, log, "MongoDBClient - DropIndex"))

  def receive = {

    case ResponseDropIndex(resultOk, errorMessage) => {
      log.info("ResponseDropIndex")

      if (resultOk) {
        log.info("ResponseDropIndex OK, creating new index...")
        documentCollection.createIndex(Indexes.descending("timestamp"),
          IndexOptions().expireAfter(settings.MongoDbExpireImagesTimeInMinutes, TimeUnit.MINUTES)).subscribe(
            new ObserveCreateIndex(this.context.self, log, "MongoDBClient - CreateIndex"))
      } else {
        log.info("ResponseDropIndex, errorMessage: " + errorMessage.getOrElse("None"))
        System.exit(1)
      }
    }

    case ResponseCreateIndex(resultOk, errorMessage) => {
      log.info("MongoDBClient - ResponseCreateIndex, resultOk: " + resultOk)

      if (resultOk == false) {
        log.info("MongoDBClient - ResponseCreateIndex, errorMessage: " + errorMessage.getOrElse("None"))
        System.exit(1)
      }
    }

    case AskTimestampImageMessage(originName, timestamp) ⇒ {
      log.debug("AskTimestampImageMessage")

      caseClassCollection.find(and(equal("originName", originName),equal("timestamp", BsonDateTime(timestamp)))).first().subscribe(
        new ObserveOneImage(sender, log, "MongoDBClient - AskTimestampImageMessage"))
    }
 
    case AskLatestImageMessage(originName) ⇒ {
      log.debug("AskImageLastMessage")

      caseClassCollection.find(equal("originName", originName)).sort(descending("timestamp")).first().subscribe(
        new ObserveOneImage(sender, log, "MongoDBClient - AskLatestImageMessage"))
    }

    case AskOldestImageMessage(originName) ⇒ {
      log.debug("AskOldestImageMessage")

      caseClassCollection.find(equal("originName", originName)).sort(ascending("timestamp")).first().subscribe(
        new ObserveOneImage(sender, log, "MongoDBClient - AskOldestImageMessage"))
    }

    case AskImagesCountMessage(originName) ⇒ {
      log.debug("AskImagesCountMessage")

      caseClassCollection.count(equal("originName", originName)).subscribe(
          new ObserveCount(sender, log, "MongoDBClient - AskImagesCountMessage"))
    }

    case AskQueryImagesListMessage(originName, imageQuery) ⇒ {
      log.debug("AskQueryImagesListMessage Start")

      val findFullBson = if (imageQuery.fromTimestamp == 0 && imageQuery.toTimestamp == 0) {
          equal("originName", originName)
        } else if (imageQuery.fromTimestamp > 0 && imageQuery.toTimestamp == 0) {
          and(gte("timestamp", BsonDateTime(imageQuery.fromTimestamp)), equal("originName", originName))
        } else if (imageQuery.fromTimestamp == 0 && imageQuery.toTimestamp > 0) {
          and(lte("timestamp", BsonDateTime(imageQuery.toTimestamp)), equal("originName", originName))
        } else {
          and(and(gte("timestamp", BsonDateTime(imageQuery.fromTimestamp)), 
              lte("timestamp", BsonDateTime(imageQuery.toTimestamp))), equal("originName", originName))
        }
      
      documentCollection.find(findFullBson).projection(include("timestamp")).sort(descending("timestamp"))
        .limit(imageQuery.limit).map[BsonDateTime] { document: Document => document("timestamp").asDateTime() }.subscribe(
          new ObserveListImageTimestamps(sender, log, "MongoDBClient - AskQueryImagesListMessage"))
    }  

    case InsertImageMessage(originName, imageData) ⇒ {
      log.debug("InsertImageMessage Start")

      caseClassCollection.insertOne(ImageDataDB(ImageDataPresented(originName, imageData.base64,
        System.currentTimeMillis))).subscribe(new ObserveInsert(sender, log, "MongoDBClient - InsertImageMessage"))
    }
    
    case InsertImageDataPresentedMessage(imageDataPresented) ⇒ {
      log.debug("InsertImageWithTimestampMessage Start")

      caseClassCollection.insertOne(ImageDataDB(imageDataPresented)).subscribe(
          new ObserveInsert(sender, log, "MongoDBClient - InsertImageMessage"))
    }
    
    case InsertImageDataPresentedGzipMessage(imageDataPresentedGzip) ⇒ {
      log.debug("InsertImageWithTimestampMessage Start")

      caseClassCollection.insertOne(ImageDataDB(imageDataPresentedGzip)).subscribe(
          new ObserveInsert(sender, log, "MongoDBClient - InsertImageMessage"))
    }    
  }
}