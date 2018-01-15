package model

case class AggretationStatus(
    originName: String,
    imagesReceived: Long,
    imagesInserted: Long,
    nrOfTimeouts: Long,
    latestTimestampReceived: Long,
    timeOfLastAggregation: Long
    ) extends HasOriginName