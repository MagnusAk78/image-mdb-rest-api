package model

import spray.json._
import spray.json.DefaultJsonProtocol._

object JsonSupport extends DefaultJsonProtocol {
  implicit val ImagesInfoFormat = jsonFormat1(ImagesInfo)
  implicit val ImageDataFormat = jsonFormat2(ImageData)
  implicit val ImageDataWithTimestampFormat = jsonFormat3(ImageDataWithTimestamp)
  implicit val ErrorMessageFormat = jsonFormat1(ErrorMessage)
  implicit val InfoMessageFormat = jsonFormat1(InfoMessage)
  implicit val ImageQueryFormat = jsonFormat4(ImageQuery)
  implicit val AggretationStatusFormat = jsonFormat6(AggretationStatus)
}