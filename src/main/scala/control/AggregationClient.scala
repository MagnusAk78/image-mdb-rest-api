package control

import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import akka.pattern._

import akka.util.Timeout
import akka.actor.Props
import scala.concurrent.duration._

import scala.concurrent.TimeoutException

import spray.http._
import spray.client.pipelining._

import scala.concurrent.{ Await, Future }

import spray.json._
//import spray.json.DefaultJsonProtocol._
import spray.httpx.SprayJsonSupport._

import model._
import model.JsonSupport._

case object AskStatusMessage

class AggregationClient(originName: String, originUrl: String, minutesBetweenCollection: Int,
    mongoDbClient: AskableActorRef) extends Actor with akka.actor.ActorLogging {

  implicit val system = ActorSystem()
  import system.dispatcher // execution context for futures

  implicit val timeout = Timeout(5 seconds)

  system.scheduler.schedule(1 minutes, minutesBetweenCollection minutes)(getImagesFromRemote)

  private var latestTimestampReceived: Long = 0
  private var imagesReceived: Long = 0
  private var imagesInserted: Long = 0
  private var nrOfTimeouts: Long = 0
  private var timeOfLastAggregation: Long = 0

  def getImagesFromRemote {
    log.debug("getImagesFromRemote START")

    try {
      timeOfLastAggregation = System.currentTimeMillis
      val latestTimestampInOwnDB = getLatestImageInOwnDB

      val timestamps = queryImages(latestTimestampInOwnDB)
      for (timestamp <- timestamps) {
        val image = getImage(timestamp)
        imagesReceived = imagesReceived + 1
        if (image.timestamp > latestTimestampReceived) {
          latestTimestampReceived = image.timestamp
        }
        writeImageData(image)
        imagesInserted = imagesInserted + 1
      }
    } catch {
      case timeout: TimeoutException => {
        nrOfTimeouts = nrOfTimeouts + 1
        log.debug("TimeoutException: " + timeout.getMessage)
      }
      case exception: Throwable => {
        log.error("ErrorMessage: " + exception.getMessage)
      }
    }
    log.debug("getImagesFromRemote END")
  }

  def getLatestImageInOwnDB: Long = {
    log.debug("getLatestImageInOwnDB START")

    val future = mongoDbClient ? AskLatestImageMessage(originName = originName)

    Await.result(future, timeout.duration).asInstanceOf[Either[ErrorMessage, ImageDataPresented]] match {
      case Left(errorMessage) => {
        log.debug("getLatestImageInOwnDB, ErrorMessage: " + errorMessage.errorMessage)
        log.debug("getLatestImageInOwnDB END")
        //Assume no match, equals 0
        0
      }
      case Right(imageDataPresented) => {
        log.debug("getLatestImageInOwnDB, imageDataPresented.timestamp: " + imageDataPresented.timestamp)
        log.debug("getLatestImageInOwnDB END")
        imageDataPresented.timestamp
      }
    }
  }

  def queryImages(latestTimestampInOwnDB: Long): List[Long] = {
    log.debug("queryImages START")

    val pipeline: HttpRequest => Future[List[Long]] = sendReceive ~> unmarshal[List[Long]]
    val response: Future[List[Long]] = pipeline(Post(originUrl + "/images/query",
      ImageQuery(originName = originName, fromTimestamp = latestTimestampInOwnDB + 1, toTimestamp = 0, limit = 0)))

    log.debug("queryImages END")
    Await.result(response, timeout.duration)
  }

  def getImage(timestamp: Long): ImageDataPresented = {
    log.debug("getImage, timestamp: " + timestamp + ", START")

    val pipeline: HttpRequest => Future[ImageDataPresented] = sendReceive ~> unmarshal[ImageDataPresented]

    val response: Future[ImageDataPresented] = pipeline(Get(originUrl + "/image/" + originName + "/" + timestamp))

    log.debug("getImage, timestamp: " + timestamp + ", END")
    Await.result(response, timeout.duration)
  }

  def writeImageData(imageDataPresented: ImageDataPresented) {
    log.debug("writeImageData START")

    val future = mongoDbClient ? InsertImageDataPresentedMessage(imageDataPresented)

    Await.result(future, timeout.duration).asInstanceOf[Either[ErrorMessage, InfoMessage]] match {
      case Left(errorMessage) => {
        log.debug("writeImageData, ErrorMessage: " + errorMessage.errorMessage)
      }
      case Right(infoMessage) => {
        log.debug("writeImageData, infoMessage: " + infoMessage.infoMessage)
      }
    }

    log.debug("writeImageData END")
  }

  def receive = {
    case AskStatusMessage => {
      sender ! AggretationStatus(originName, imagesReceived, imagesInserted, nrOfTimeouts, latestTimestampReceived,
        timeOfLastAggregation)
    }
  }
}