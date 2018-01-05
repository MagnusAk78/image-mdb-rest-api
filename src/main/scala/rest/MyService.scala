package rest

import akka.actor.{ Actor, ActorRef }
import scala.concurrent.Await
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import spray.routing._
import spray.http._
import MediaTypes._

import spray.json._

import model.JsonSupport._
import model.ImageData
import model.ImagesInfo
import model.ErrorMessage
import control._

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class MyServiceActor(override val mongoDBClient: ActorRef) extends Actor with MyService {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  def receive = runRoute(myRoute)
}

trait MyService extends HttpService {

  val mongoDBClient: ActorRef

  implicit val timeout = Timeout(5 seconds)

  val myRoute = {
    import spray.httpx.SprayJsonSupport.sprayJsonMarshaller
    import spray.httpx.SprayJsonSupport.sprayJsonUnmarshaller

    pathPrefix("image") {
      path(LongNumber) { timestamp =>
        get {
          respondWithMediaType(`application/json`) {
            complete {
              val future = mongoDBClient ? AskImageTimestampMessage(timestamp)
              val result = Await.result(future, timeout.duration).asInstanceOf[Either[ImageData, ErrorMessage]]
              result match {
                case Left(imageData) => imageData
                case Right(errorMessage) => errorMessage
              }
            }
          }
        }
      } ~
      path("last") {
        get {
          respondWithMediaType(`application/json`) {
            complete {
              val future = mongoDBClient ? AskImageLastMessage
              val result = Await.result(future, timeout.duration).asInstanceOf[Either[ImageData, ErrorMessage]]
              result match {
                case Left(imageData) => imageData
                case Right(errorMessage) => errorMessage
              }
            }
          }
        }
      }
    } ~
    pathPrefix("images") {
    	path("count") {
    		get {
    			respondWithMediaType(`application/json`) {
    				complete {
    					val future = mongoDBClient ? AskImagesCountMessage
    							val result = Await.result(future, timeout.duration).asInstanceOf[ImagesInfo]
    									result
    				}
    			}
    		}
    	} ~
    	path("list") {
    		get {
    			respondWithMediaType(`application/json`) {
    				complete {
    					val future = mongoDBClient ? AskImagesListMessage
    							val result = Await.result(future, timeout.duration).asInstanceOf[List[Long]]
    									result
    				}
    			}
    		}
    	}
    } ~
    path("") {
    	get {
    		respondWithMediaType(`text/html`) { // XML is marshalled to `text/xml` by default, so we simply override here
    			complete {
    				"""
    				<html>
    				<body>
    				<h1>Image MongoDB REST API</h1>
    				<br>
    				/image/timestamp:Long <br>
    				/image/last <br>
    				/images/count <br>
    				/images/list <br>
    				</body>
    				</html>
    				"""
    			}
    		}
    	}
    }
  }
}