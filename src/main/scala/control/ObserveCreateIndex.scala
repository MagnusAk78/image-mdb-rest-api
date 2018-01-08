package control

import scala.Left
import scala.Right

import org.mongodb.scala.Observer

import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import akka.event.LoggingAdapter
import model.ErrorMessage
import model.ImagesInfo

class ObserveCreateIndex(sender: ActorRef, log: LoggingAdapter, caller: String) extends Observer[String] {

  var resultSent = false

  override def onNext(result: String): Unit = {
    log.info(caller + ", onNext: " + result)
    resultSent = true
    
    sender ! ResponseCreateIndex(true, None)
  }

  override def onError(e: Throwable): Unit = {
    resultSent = true
    log.info(caller + ", onError: " + e.getMessage)
    sender ! ResponseCreateIndex(false, Some(e.getMessage))
  }

  override def onComplete(): Unit = {
    log.info(caller + ", onComplete")
    
    if (resultSent == false) {
      sender ! ResponseCreateIndex(false, Some("Unexpected complete"))
    }
  }  
}