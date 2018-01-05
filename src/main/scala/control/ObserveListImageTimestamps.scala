package control

import scala.Left
import scala.Right

import org.mongodb.scala.Observer

import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import akka.event.LoggingAdapter
import model.ErrorMessage
import model.ImageDataDB

class ObserveListImageTimestamps(sender: ActorRef, log: LoggingAdapter, caller: String) extends Observer[Long] {

  var timestampList: List[Long] = List.empty

  override def onNext(timestamp: Long): Unit = {
    log.info(caller + ", onNext: " + timestamp)
    timestampList = timestampList match {
      case Nil => List(timestamp)
      case list => list ++ List(timestamp)
    }    
  }

  override def onError(e: Throwable): Unit = {
    log.info(caller + ", onError: " + e.getMessage) 
    sender ! Left(ErrorMessage("database error: e.getMessage"))
  }

  override def onComplete(): Unit = {
    log.info(caller + ", onComplete")
    
    sender ! Right(timestampList)
  }
}