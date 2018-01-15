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

  implicit val timeout = Timeout(10 seconds)

  system.scheduler.schedule(0 minutes, minutesBetweenCollection minutes)(queryImages)

  private var latestTimestampReceived: Long = 0
  private var imagesReceived: Long = 0
  private var imagesInserted: Long = 0
  private var nrOfTimeouts: Long = 0
  private var timeOfLastAggregation: Long = 0

  def queryImages {
    timeOfLastAggregation = System.currentTimeMillis
    
    val pipeline: HttpRequest => Future[List[Long]] = sendReceive ~> unmarshal[List[Long]]
    val response: Future[List[Long]] = pipeline(Post(originUrl + "/images/query",
      ImageQuery(originName = originName, fromTimestamp = latestTimestampReceived + 1, toTimestamp = 0, limit = 0)))

      try {
        val timestamps = Await.result(response, timeout.duration)
        for(timestamp <- timestamps) {
          getImage(timestamp)
        }
      } catch {
      case timeout: TimeoutException => {
        nrOfTimeouts = nrOfTimeouts + 1
        log.info("AggregationClient, queryImages, TimeoutException")
      }
      case exception: Throwable => {
        log.error("AggregationClient, queryImages, ErrorMessage: " + exception.getMessage)
      }
    }
    
  }

  def getImage(timestamp: Long) {
    val pipeline: HttpRequest => Future[ImageDataWithTimestamp] = sendReceive ~> unmarshal[ImageDataWithTimestamp]

    val response: Future[ImageDataWithTimestamp] = pipeline(Get(originUrl + "/image/" + timestamp))

    try {
      writeImageData(Await.result(response, timeout.duration))
    } catch {
      case timeout: TimeoutException => {
        nrOfTimeouts = nrOfTimeouts + 1
        log.info("AggregationClient, getImage, TimeoutException")
      }
      case exception: Throwable => {
        log.error("AggregationClient, getImage, ErrorMessage: " + exception.getMessage)
      }
    }

  }

  def writeImageData(imageDataWithTimestamp: ImageDataWithTimestamp) {
    imagesReceived = imagesReceived + 1
    if(imageDataWithTimestamp.timestamp > latestTimestampReceived) {
      latestTimestampReceived = imageDataWithTimestamp.timestamp  
    }
    val future = mongoDbClient ? InsertImageWithTimestampMessage(imageDataWithTimestamp)

    try {
      Await.result(future, timeout.duration).asInstanceOf[Either[ErrorMessage, InfoMessage]] match {
        case Left(errorMessage) => {
          log.error("AggregationClient, writeImageData, ErrorMessage: " + errorMessage.errorMessage)
        }
        case Right(infoMessage) => {
          imagesInserted = imagesInserted + 1
          log.debug("AggregationClient, writeImageData, infoMessage: " + infoMessage.infoMessage)
        }
      }
    } catch {
      case timeout: TimeoutException => {
        nrOfTimeouts = nrOfTimeouts + 1
        log.info("AggregationClient, writeImageData, TimeoutException")
      }
      case exception: Throwable => {
        log.error("AggregationClient, writeImageData, ErrorMessage: " + exception.getMessage)
      }
    }

  }

  def receive = {
    case AskStatusMessage => {
      sender ! AggretationStatus(originName, imagesReceived, imagesInserted, nrOfTimeouts, latestTimestampReceived,
      timeOfLastAggregation)
    }
  }
}