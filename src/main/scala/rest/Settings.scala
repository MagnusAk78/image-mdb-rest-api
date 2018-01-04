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
  val ServerBindIp: String = config.getString("ifm-master-server.bind-server-ip")
  val ServerBindPort: Int = config.getInt("ifm-master-server.bind-server-port")
  val MaxNumberOfProcessData: Int = config.getInt("ifm-master-server.max-number-of-process-data")
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