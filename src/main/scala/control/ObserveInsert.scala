package control

import scala.Left
import scala.Right

import org.mongodb.scala.Observer
import org.mongodb.scala.Completed

import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import akka.event.LoggingAdapter
import model.ErrorMessage
import model.InfoMessage

class ObserveInsert(sender: ActorRef, log: LoggingAdapter, caller: String) extends Observer[Completed] {

  var resultSent = false

  override def onNext(result: Completed): Unit = {
    log.debug(caller + ", onNext: " + result)
    resultSent = true
    
    sender ! Right(InfoMessage("Insert OK"))
  }

  override def onError(e: Throwable): Unit = {
    resultSent = true
    log.debug(caller + ", onError: " + e.getMessage) 
    sender ! Left(ErrorMessage("database error: e.getMessage"))
  }

  override def onComplete(): Unit = {
    log.debug(caller + ", onComplete")
    
    if (resultSent == false) {
      sender ! Left(ErrorMessage("unexpected complete"))
    }
  }  
}