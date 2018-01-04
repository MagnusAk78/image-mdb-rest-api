package model

import spray.json._
import spray.json.DefaultJsonProtocol._

object JsonSupport extends DefaultJsonProtocol {
  implicit val ImageDataFormat = jsonFormat2(ImageData)
  implicit val ImagesInfoFormat = jsonFormat1(ImagesInfo)
}