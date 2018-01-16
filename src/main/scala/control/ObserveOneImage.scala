package control

import scala.Left
import scala.Right

import org.mongodb.scala.Observer

import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import akka.event.LoggingAdapter
import model.ErrorMessage
import model.ImageDataDB

class ObserveOneImage(sender: ActorRef, log: LoggingAdapter, caller: String) extends Observer[ImageDataDB] {

  var resultSent = false

  override def onNext(imageDataDB: ImageDataDB): Unit = {
    log.debug(caller + ", onNext, image timestamp " + imageDataDB.timestamp)
    resultSent = true
    
    sender ! Right(ImageDataDB.toImageDataPresented(imageDataDB))
  }

  override def onError(e: Throwable): Unit = {
    resultSent = true
    log.debug(caller + ", onError: " + e.getMessage) 
    sender ! Left(ErrorMessage("database error: " + e.getMessage))
  }

  override def onComplete(): Unit = {
    log.debug(caller + ", onComplete")
    
    if (resultSent == false) {
      sender ! Left(ErrorMessage("No match for queried document"))
    }
  }
}