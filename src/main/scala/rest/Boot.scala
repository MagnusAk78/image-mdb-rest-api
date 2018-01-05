package rest

import akka.actor.{ ActorSystem, Props }
import akka.io.{ IO, Tcp }
import spray.can.Http
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import java.net.InetSocketAddress

import control.MongoDBClient

object Boot extends App {

  import Tcp._

  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("on-spray-can")
  val settings = Settings(system)

  // Mongo DB client actor
  val mongoDBClient = system.actorOf(Props(new MongoDBClient(settings)), "Mongo-Db-client")

  // create and start our service actor
  val service = system.actorOf(Props(new MyServiceActor(mongoDBClient)), "image-mdb-rest-api")

  implicit val timeout = Timeout(10.seconds)
  
  // start a new HTTP server
  IO(Http) ? Http.Bind(service, interface = settings.HttpServerBindIp, port = settings.HttpServerBindPort)
}
