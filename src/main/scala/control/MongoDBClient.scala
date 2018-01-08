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

case class AskTimestampImageMessage(timestamp: Long)
case object AskLatestImageMessage
case object AskOldestImageMessage

case object AskImagesCountMessage
case class AskQueryImagesListMessage(imageQuery: ImageQuery)

case class InsertImageMessage(imageData: ImageData)

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
      log.info("MongoDBClient - ResponseDropIndex")

      if (resultOk) {
        log.info("MongoDBClient - ResponseDropIndex OK, creating new index...")
        documentCollection.createIndex(Indexes.descending("timestamp"),
          IndexOptions().expireAfter(settings.MongoDbExpireImagesTimeInMinutes, TimeUnit.MINUTES)).subscribe(
            new ObserveCreateIndex(this.context.self, log, "MongoDBClient - CreateIndex"))
      } else {
        log.info("MongoDBClient - ResponseDropIndex, errorMessage: " + errorMessage.getOrElse("None"))
        System.exit(1)
      }
    }

    case ResponseCreateIndex(resultOk, errorMessage) => {
      log.info("MongoDBClient - ResponseCreateIndex")
      log.info("MongoDBClient - ResponseCreateIndex, resultOk: " + resultOk)

      if (resultOk == false) {
        log.info("MongoDBClient - ResponseCreateIndex, errorMessage: " + errorMessage.getOrElse("None"))
        System.exit(1)
      }
    }

    case AskTimestampImageMessage(timestamp) ⇒ {
      log.info("MongoDBClient - AskTimestampImageMessage")

      caseClassCollection.find(equal("timestamp", timestamp)).first().subscribe(
        new ObserveOneImage(sender, log, "MongoDBClient - AskTimestampImageMessage"))
    }

    case AskLatestImageMessage ⇒ {
      log.info("MongoDBClient - AskImageLastMessage")

      caseClassCollection.find.sort(descending("timestamp")).first().subscribe(
        new ObserveOneImage(sender, log, "MongoDBClient - AskLatestImageMessage"))
    }

    case AskOldestImageMessage ⇒ {
      log.info("MongoDBClient - AskOldestImageMessage")

      caseClassCollection.find.sort(ascending("timestamp")).first().subscribe(
        new ObserveOneImage(sender, log, "MongoDBClient - AskOldestImageMessage"))
    }

    case AskImagesCountMessage ⇒ {
      log.info("MongoDBClient - AskImagesCountMessage")

      caseClassCollection.count().subscribe(new ObserveCount(sender, log, "MongoDBClient - AskImagesCountMessage"))
    }

    case AskQueryImagesListMessage(imageQuery) ⇒ {
      log.info("MongoDBClient - AskQueryImagesListMessage Start")

      val findBson = if (imageQuery.fromTimestamp == 0 && imageQuery.toTimestamp == 0) {
        Document.empty
      } else if (imageQuery.fromTimestamp > 0 && imageQuery.toTimestamp == 0) {
        gte("timestamp", imageQuery.fromTimestamp)
      } else if (imageQuery.fromTimestamp == 0 && imageQuery.toTimestamp > 0) {
        lte("timestamp", imageQuery.toTimestamp)
      } else {
        and(gte("timestamp", imageQuery.fromTimestamp), lte("timestamp", imageQuery.toTimestamp))
      }

      documentCollection.find(findBson).projection(include("timestamp")).sort(descending("timestamp"))
        .limit(imageQuery.limit).map[BsonDateTime] { document: Document => document("timestamp").asDateTime() }.subscribe(
          new ObserveListImageTimestamps(sender, log, "MongoDBClient - AskQueryImagesListMessage"))
    }

    case InsertImageMessage(imageData) ⇒ {
      log.info("MongoDBClient - InsertImageMessage Start")

      caseClassCollection.insertOne(ImageDataDB(ImageDataWithTimestamp(imageData.base64, System.currentTimeMillis))).subscribe(
        new ObserveInsert(sender, log, "MongoDBClient - InsertImageMessage"))
    }
  }
}