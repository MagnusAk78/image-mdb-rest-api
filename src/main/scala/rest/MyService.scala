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
import model.InfoMessage
import model.ImageDataWithTimestamp
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
              val future = mongoDBClient ? AskTimestampImageMessage(timestamp)
              val result = Await.result(future, timeout.duration).asInstanceOf[Either[ErrorMessage, ImageDataWithTimestamp]]
              result match {
                case Left(errorMessage) => errorMessage
                case Right(imageData) => imageData
              }
            }
          }
        }
      } ~
      path("latest") {
        get {
          respondWithMediaType(`application/json`) {
            complete {
              val future = mongoDBClient ? AskLatestImageMessage
              val result = Await.result(future, timeout.duration).asInstanceOf[Either[ErrorMessage, ImageDataWithTimestamp]]
              result match {
                case Left(errorMessage) => errorMessage
                case Right(imageData) => imageData
              }
            }
          }
        }
      } ~
      path("oldest") {
        get {
          respondWithMediaType(`application/json`) {
            complete {
              val future = mongoDBClient ? AskOldestImageMessage
              val result = Await.result(future, timeout.duration).asInstanceOf[Either[ErrorMessage, ImageDataWithTimestamp]]
              result match {
                case Left(errorMessage) => errorMessage
                case Right(imageData) => imageData
              }
            }
          }
        }
      } ~
      path("insert") {
        post {
          entity(as[model.ImageData]) { imageData => 
            respondWithMediaType(`application/json`) {
              complete {
                val future = mongoDBClient ? InsertImageMessage(imageData)
                val result = Await.result(future, timeout.duration).asInstanceOf[Either[ErrorMessage, InfoMessage]]
                result match {
                  case Left(errorMessage) => errorMessage
                  case Right(infoMessage) => infoMessage
                }
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
    					val result = Await.result(future, timeout.duration).asInstanceOf[Either[ErrorMessage, ImagesInfo]]
    					result match {
                case Left(errorMessage) => errorMessage
                case Right(imagesInfo) => imagesInfo
              }
    				}
    			}
    		}
    	} ~
    	path("query") {
    		post {
    		  entity(as[model.ImageQuery]) { imageQuery =>
    			  respondWithMediaType(`application/json`) {
    				  complete {
    					  val future = mongoDBClient ? AskQueryImagesListMessage(imageQuery)
    					  val result = Await.result(future, timeout.duration).asInstanceOf[Either[ErrorMessage, List[Long]]]
    					  result match {
                  case Left(errorMessage) => errorMessage
                  case Right(list) => list
                }
    				  }
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
    				  <head>
                <title>Image MongoDB REST API</title>
              </head>
    				  <body>
    				    <h2>Image MongoDB REST API</h2>
    				    
    				    <h4>Get image with specific timestamp</h4>
    				    <code>GET:  /image/&lt;timestamp&gt;</code><br>
    				    
    				    <h4>Get latest inserted image</h4>
    				    <code>GET:  /image/latest</code> <br>
    				    
    				    <h4>Get oldest inserted image</h4>
    				    <code>GET:  /image/oldest</code> <br>
    				    
    				    <h4>Insert new image</h4>
    				    <code>POST: /image/insert</code> <br>
    				    
    				    <code>{"base64": &lt;Base64String&gt;}</code>
    				    <p>Upload an image as an base64 encoded string.</p>
    				    
    				    <h4>Count current number of images stored</h4>
    				    <code>GET:  /images/count</code> <br>
    				    
    				    <h4>Query images</h4>
    				    <code>POST: /images/query</code> <br>
    				    
    				    <code>{"fromTimestamp": &lt;timestamp&gt;,"toTimestamp": &lt;timestamp&gt;, "limit": &lt;limit&gt;}</code><br>
    				    <p>Assign 0 to any field to ignore it.</p>
    				  </body>
    				</html>
    				"""
    			}
    		}
    	}
    }
  }
}