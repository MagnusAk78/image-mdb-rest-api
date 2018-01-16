package model

import spray.json._
import spray.json.DefaultJsonProtocol._

object JsonSupport extends DefaultJsonProtocol {
  implicit val ImagesInfoFormat = jsonFormat1(ImagesInfo)
  implicit val ImageDataFormat = jsonFormat1(ImageDataInsert)
  implicit val ImageDataPresentedFormat = jsonFormat3(ImageDataPresented)
  implicit val ErrorMessageFormat = jsonFormat1(ErrorMessage)
  implicit val InfoMessageFormat = jsonFormat1(InfoMessage)
  implicit val ImageQueryFormat = jsonFormat3(ImageQuery)
  implicit val AggretationStatusFormat = jsonFormat6(AggretationStatus)
}