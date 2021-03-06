package control

import scala.Left
import scala.Right

import org.mongodb.scala.Observer
import org.mongodb.scala.bson.BsonDateTime

import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import akka.event.LoggingAdapter
import model.ErrorMessage
import model.ImageDataDB

class ObserveListImageTimestamps(sender: ActorRef, log: LoggingAdapter, caller: String) extends Observer[BsonDateTime] {

  var timestampList: List[Long] = List.empty

  override def onNext(timestamp: BsonDateTime): Unit = {
    log.debug(caller + ", onNext: " + timestamp)
    timestampList = timestampList match {
      case Nil => List(timestamp.getValue)
      case list => list ++ List(timestamp.getValue)
    }    
  }

  override def onError(e: Throwable): Unit = {
    log.debug(caller + ", onError: " + e.getMessage) 
    sender ! Left(ErrorMessage("database error: e.getMessage"))
  }

  override def onComplete(): Unit = {
    log.debug(caller + ", onComplete")
    
    sender ! Right(timestampList)
  }
}