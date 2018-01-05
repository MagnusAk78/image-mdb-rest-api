package rest

import akka.actor.ActorSystem
import akka.actor.Extension
import akka.actor.ExtensionId
import akka.actor.ExtensionIdProvider
import akka.actor.ExtendedActorSystem
import scala.concurrent.duration.Duration
import com.typesafe.config.Config
import java.util.concurrent.TimeUnit
 
class SettingsImpl(config: Config) extends Extension {
  val HttpServerBindIp: String = config.getString("spray.can.server.bind-server-ip")
  val HttpServerBindPort: Int = config.getInt("spray.can.server.bind-server-port")
  val MongoDbIpAddress: String = config.getString("mongo-db.ip-address")
  val MongoDbPort: String = config.getString("mongo-db.port")
  val MongoDbDatabaseName: String = config.getString("mongo-db.database-name")
  val MongoDbCollectionName: String = config.getString("mongo-db.collection-name")
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