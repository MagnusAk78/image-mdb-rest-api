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
import model.ImageDataInsert
import model.ImagesInfo
import model.ErrorMessage
import model.InfoMessage
import model.ImageDataPresented
import model.AggretationStatus
import control._

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class MyServiceActor(override val mongoDBClient: ActorRef, override val aggregationClients: List[ActorRef])
    extends Actor with MyService {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  def receive = runRoute(myRoute)
}

trait MyService extends HttpService {

  val mongoDBClient: ActorRef
  val aggregationClients: List[ActorRef]

  implicit val timeout = Timeout(5 seconds)

  val myRoute = {
    import spray.httpx.SprayJsonSupport.sprayJsonMarshaller
    import spray.httpx.SprayJsonSupport.sprayJsonUnmarshaller

    pathPrefix(Segment) { originName =>
      pathPrefix("image") {
        path(LongNumber) { timestamp =>
          get {
            respondWithMediaType(`application/json`) {
              complete {
                val future = mongoDBClient ? AskTimestampImageMessage(originName, timestamp)
                val result = Await.result(future, timeout.duration).asInstanceOf[Either[ErrorMessage, ImageDataPresented]]
                result match {
                  case Left(errorMessage) => errorMessage
                  case Right(imageDataPresented) => imageDataPresented
                }
              }
            }
          }
        } ~
        path("latest") {
          get {
            respondWithMediaType(`application/json`) {
              complete {
                val future = mongoDBClient ? AskLatestImageMessage(originName)
                val result = Await.result(future, timeout.duration).asInstanceOf[Either[ErrorMessage, ImageDataPresented]]
                result match {
                  case Left(errorMessage) => errorMessage
                  case Right(imageDataPresented) => imageDataPresented
                }
              }
            }
          }
        } ~
        path("oldest") {
          get {
            respondWithMediaType(`application/json`) {
              complete {
                val future = mongoDBClient ? AskOldestImageMessage(originName)
                val result = Await.result(future, timeout.duration).asInstanceOf[Either[ErrorMessage, ImageDataPresented]]
                result match {
                  case Left(errorMessage) => errorMessage
                  case Right(imageDataPresented) => imageDataPresented
                }
              }
            }
          }
        } ~
        path("insert") {
          post {
            entity(as[model.ImageDataInsert]) { imageData =>
              respondWithMediaType(`application/json`) {
                complete {
                  val future = mongoDBClient ? InsertImageMessage(originName, imageData)
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
      					val future = mongoDBClient ? AskImagesCountMessage(originName)
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
      					  val future = mongoDBClient ? AskQueryImagesListMessage(originName, imageQuery)
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
      }
    } ~
    pathPrefix("aggregation") {
    	path("status") {
    		get {
    			respondWithMediaType(`application/json`) {
    				complete {
    					val futures = aggregationClients.map { _ ? AskStatusMessage }
    					futures.map { future => Await.result(future, timeout.duration).asInstanceOf[AggretationStatus] }
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

    				<h4>Get image from a specific origin with specific timestamp</h4>
    				<code>GET:  /&lt;originName&gt;/image/&lt;timestamp&gt;</code><br>

    				<h4>Get latest inserted image from a specific origin</h4>
    				<code>GET:  &lt;originName&gt;/image/latest</code> <br>

    				<h4>Get oldest inserted image from a specific origin</h4>
    				<code>GET:  /&lt;originName&gt;/image/oldest</code> <br>

    				<h4>Insert new image from a specific origin</h4>
    				<code>POST: /&lt;originName&gt;/image/insert</code> <br>

    				<code><pre>
    				{
    				"base64": Image data, &lt;Base64String&gt;
    				}
    				</pre></code>
    				<p>Upload an image from a specific origin as an base64 encoded string.</p>

    				<h4>Count current number of images stored from a specific origin</h4>
    				<code>GET:  /&lt;originName&gt;/images/count</code> <br>

    				<h4>Query images</h4>
    				<code>POST: /&lt;originName&gt;/images/query</code> <br>

    				<code><pre>
    				{
    				"fromTimestamp": &lt;timestamp&gt;,
    				"toTimestamp": &lt;timestamp&gt;,
    				"limit": &lt;limit&gt;
    				}
    				</pre></code>
    				<p>Assign 0 to any field to ignore it.</p> 
    				<br>

    				<h4>Get status of eventual aggregation</h4>
    				<code>GET: /aggregation/status</code> <br>

    				</pre></code>
    				<p>Assign 0 or empty to any field to ignore it.</p>    				    
    				</body>
    				</html>
    				"""
    			}
    		}
    	}
    }
  }
}