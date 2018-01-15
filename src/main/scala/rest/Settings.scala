package rest

import akka.actor.ActorSystem
import akka.actor.Extension
import akka.actor.ExtensionId
import akka.actor.ExtensionIdProvider
import akka.actor.ExtendedActorSystem
import scala.concurrent.duration.Duration
import com.typesafe.config._
import java.util.concurrent.TimeUnit
import scala.collection.JavaConverters._ 
import java.util.Map.Entry
 
class SettingsImpl(config: Config) extends Extension {
  val HttpServerBindIp: String = config.getString("spray.can.server.bind-server-ip")
  val HttpServerBindPort: Int = config.getInt("spray.can.server.bind-server-port")
  val MongoDbIpAddress: String = config.getString("mongo-db.ip-address")
  val MongoDbPort: String = config.getString("mongo-db.port")
  val MongoDbDatabaseName: String = config.getString("mongo-db.database-name")
  val MongoDbCollectionName: String = config.getString("mongo-db.collection-name")
  val MongoDbExpireImagesTimeInMinutes: Long = config.getLong("mongo-db.expire-after-minutes")
  lazy val Origins : Map[String, String] = {
    val listOfOrigins: List[ConfigObject] = config.getObjectList("aggregation.list-of-oigins").asScala.toList
    (for {
      item: ConfigObject <- listOfOrigins
      entry : Entry[String, ConfigValue] <- item.entrySet().asScala
      key = entry.getKey
      uri = entry.getValue.unwrapped().toString
    } yield(key, uri)).toMap 
  }
  val minutesBetweenCollection: Int = config.getInt("aggregation-db.minutes-between-collection")
}

object Settings extends ExtensionId[SettingsImpl] with ExtensionIdProvider {
 
  override def lookup = Settings
 
  override def createExtension(system: ExtendedActorSystem) =
    new SettingsImpl(system.settings.config)
 
  /**
   * Java API: retrieve the Settings extension for the given system.
   */
  override def get(system: ActorSystem): SettingsImpl = super.get(system)
}