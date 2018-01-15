organization  := "com.example"

version       := "0.1"

scalaVersion  := "2.11.6"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers += "spray repo" at "http://repo.spray.io"
resolvers += "spray nightlies repo" at "http://nightlies.spray.io"

libraryDependencies ++= {
  val akkaV = "2.3.9"
  val sprayV = "1.3.4"
  Seq(
    "io.spray"            %%  "spray-can"     % sprayV,
    "io.spray"            %%  "spray-routing" % sprayV,
    "io.spray"            %%  "spray-http" % sprayV,
    "io.spray"            %%  "spray-httpx" % sprayV,
    "io.spray"            %%  "spray-util" % sprayV,
    "io.spray"            %%  "spray-client" % sprayV,
    "io.spray"            %%  "spray-json" % sprayV,
    "io.spray"            %%  "spray-testkit" % sprayV  % "test",
    "com.typesafe.akka"   %%  "akka-actor"    % akkaV,
    "com.typesafe.akka"   %%  "akka-testkit"  % akkaV   % "test",
    "org.specs2"          %%  "specs2-core"   % "2.3.11" % "test",
    "org.mongodb.scala"   %%  "mongo-scala-driver" % "2.1.0"
  )
}
