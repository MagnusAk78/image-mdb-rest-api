package rest

import akka.actor.{ ActorSystem, Props }
import akka.io.{ IO, Tcp }
import spray.can.Http
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import java.net.InetSocketAddress

import control.MongoDBClient
import control.AggregationClient

object Boot extends App {

  import Tcp._

  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("image-dbm")
  val settings = Settings(system)

  // Mongo DB client actor
  val mongoDBClient = system.actorOf(Props(new MongoDBClient(settings)), "Mongo-Db-client")
  
  // Aggregation client actors
  val listOfAgggregationClientActors = settings.Origins.map { origin: (String, String) => 
    system.actorOf(Props(new AggregationClient(origin._1, origin._2, settings.minutesBetweenCollection, mongoDBClient)), 
        "Aggregation-client") }.toList

  // create and start our service actor
  val service = system.actorOf(Props(new MyServiceActor(mongoDBClient, listOfAgggregationClientActors)), 
      "image-mdb-rest-api")

  implicit val timeout = Timeout(10.seconds)
  
  // start a new HTTP server
  IO(Http) ? Http.Bind(service, interface = settings.HttpServerBindIp, port = settings.HttpServerBindPort)
}
