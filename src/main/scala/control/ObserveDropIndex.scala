package control

import scala.Left
import scala.Right

import org.mongodb.scala.Observer

import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import akka.event.LoggingAdapter
import model.ErrorMessage
import model.ImagesInfo
import org.mongodb.scala.Completed

class ObserveDropIndex(sender: ActorRef, log: LoggingAdapter, caller: String) extends Observer[Completed] {

  var resultSent = false

  override def onNext(result: Completed): Unit = {
    log.info(caller + ", onNext: " + result)
    resultSent = true
    
    sender ! ResponseDropIndex(true, None)
  }

  override def onError(e: Throwable): Unit = {
    resultSent = true
    log.info(caller + ", onError: " + e.getMessage)
    sender ! ResponseDropIndex(false, Some(e.getMessage))
  }

  override def onComplete(): Unit = {
    log.info(caller + ", onComplete")
    
    if (resultSent == false) {
      sender ! ResponseDropIndex(false, Some("Unexpected complete"))
    }
  }  
}