package control

import scala.Left
import scala.Right

import org.mongodb.scala.Observer

import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import akka.event.LoggingAdapter
import model.ErrorMessage
import model.ImagesInfo

class ObserveCount(sender: ActorRef, log: LoggingAdapter, caller: String) extends Observer[Long] {

  var resultSent = false

  override def onNext(countValue: Long): Unit = {
    log.debug(caller + ", onNext: " + countValue)
    resultSent = true
    
    sender ! Right(ImagesInfo(count = countValue))
  }

  override def onError(e: Throwable): Unit = {
    resultSent = true
    log.debug(caller + ", onError: " + e.getMessage) 
    sender ! Left(ErrorMessage("database error: e.getMessage"))
  }

  override def onComplete(): Unit = {
    log.debug(caller + ", onComplete")
    
    if (resultSent == false) {
      sender ! Right(ImagesInfo(count = 0))
    }
  }  
}