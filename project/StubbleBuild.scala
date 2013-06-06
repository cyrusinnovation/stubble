import com.github.siasia.{WebPlugin, PluginKeys}
import sbt._
import Keys._
import sbt.Tests.{Setup, Cleanup}
import com.typesafe.sbt.SbtPgp
import com.typesafe.sbt.pgp.PgpKeys._

object StubbleBuild extends Build {
  lazy val buildSettings = Seq(
    organization := "com.cyrusinnovation",
    name := "Stubble",
    version := "0.0.3",
    scalaVersion := "2.9.2",
    crossScalaVersions := Seq("2.9.2", "2.10.0"),
    resolvers ++= Seq("twttr" at "http://maven.twttr.com/"),
    parallelExecution in Test := false,
    scalacOptions += "-deprecation",
    publishMavenStyle := true,
    organizationName := "Cyrus Innovation",
    organizationHomepage := Some(url("http://www.cyrusinnovation.com/")),
    publishTo <<= version {
                            (v: String) =>
                              val nexus = "https://oss.sonatype.org/"
                              if (v.trim.endsWith("SNAPSHOT"))
                                Some("snapshots" at nexus + "content/repositories/snapshots")
                              else
                                Some("releases" at nexus + "service/local/staging/deploy/maven2")
                          },
    publishArtifact in Test := false,
    publishArtifact in IntegrationTest := false,
//    SbtPgp.usePgpKeyHex("4C046D059F897ED9"),
    pomIncludeRepository := {
      _ => false
    },
    licenses := Seq("The MIT License" -> url("http://www.opensource.org/licenses/mit-license.php")),
    homepage := Some(url("https://www.github.com/cyrusinnovation/stubble")),
    pomExtra := (
        <scm>
          <url>https://github.com/cyrusinnovation/stubble</url>
          <connection>scm:git:git@github.com:cyrusinnovation/stubble.git</connection>
          <developerConnection>scm:git:git@github.com:cyrusinnovation/stubble.git</developerConnection>
          <tag>HEAD</tag>
        </scm>
        <developers>
          <developer>
            <id>ostewart</id>
            <name>Oliver Stewart</name>
            <email>ostewart@cyrusinnovation.com</email>
            <organization>Cyrus Innovation</organization>
            <organizationUrl>http://www.cyrusinnovation.com/</organizationUrl>
            <timezone>America/New_York</timezone>
          </developer>
        </developers>
      )

  )
  override lazy val settings = super.settings ++ buildSettings

  lazy val root = Project(id = "stubble",
                          base = file("."),
                          settings = Project.defaultSettings)
    .aggregate(core)
  lazy val core = Project(id = "stubble-core",
                          base = file("core"),
                          settings = Project.defaultSettings ++ Seq(
                            libraryDependencies ++= Dependencies.core
                          ))
    .configs(IntegrationTest)
    .settings(libraryDependencies ++= Dependencies.allJunit)
    .settings(Defaults.itSettings: _*)
    .settings(startStubble <<= (fullClasspath in IntegrationTest, runner, compile in IntegrationTest) map {
    (cp, runner, _) => {
      Run.run("com.cyrusinnovation.stubble.integrationtest.StartTestServers", cp.files, Seq(), ConsoleLogger())(runner)
    }
  })
    .settings(stopStubble <<= (fullClasspath in IntegrationTest, runner, compile in IntegrationTest) map {
    (cp, runner, _) => {
      Run.run("com.cyrusinnovation.stubble.integrationtest.StopTestServers", cp.files, Seq(), ConsoleLogger())(runner)
    }
  })
    .settings(testOptions in IntegrationTest += new Setup(loader => {
    val clazz = loader.loadClass("com.cyrusinnovation.stubble.integrationtest.StartTestServers$")
    clazz.getDeclaredMethod("startStubble").invoke(clazz.getField("MODULE$").get(clazz))
  }))
    .settings(testOptions in IntegrationTest += new Cleanup(loader => {
    val clazz = loader.loadClass("com.cyrusinnovation.stubble.integrationtest.StopTestServers$")
    clazz.getDeclaredMethod("stopStubble").invoke(clazz.getField("MODULE$").get(clazz))
  }))

  val startStubble = TaskKey[Unit]("start-stubble", "Starts an instance of the Stubble server")
  val stopStubble = TaskKey[Unit]("stop-stubble", "Stops a running instance of the Stubble server")


  lazy val testServer = Project(id = "test-server",
                                base = file("test-server"),
                                settings = Project.defaultSettings)
    .settings(com.github.siasia.WebPlugin.webSettings: _*)
    .settings(libraryDependencies += "org.mortbay.jetty" % "jetty" % "6.1.22" % "container")
    .settings(libraryDependencies ++= Dependencies.testServer)
    .settings(PluginKeys.port in WebPlugin.container.Configuration := 8081)

}


object Dependencies {
  val slf4jVersion = "1.6.1"
  val finagleVersion = "6.2.1"

  object Compile {
    val jodaTime = "joda-time" % "joda-time" % "1.6.2"
    val logback = "ch.qos.logback" % "logback-classic" % "0.9.25"
    val slf4jApi = "org.slf4j" % "slf4j-api" % slf4jVersion
    val liftJson = "net.liftweb" %% "lift-json" % "2.5-RC2"
    val finagleCore = "com.twitter" %% "finagle-core" % finagleVersion
    val finagleHttp = "com.twitter" %% "finagle-http" % finagleVersion
  }

  object Test {
    val junit = "junit" % "junit" % "4.7" % "test,it"
    val junitInterface = "com.novocode" % "junit-interface" % "0.8" % "test,it"
  }

  object Provided {
    val servletApi = "javax.servlet" % "javax.servlet-api" % "3.0.1"
  }

  import Compile._
  import Test._
  import Provided._

  lazy val allJunit = Seq(junit, junitInterface)
  lazy val core = Seq(finagleCore, finagleHttp, logback, jodaTime, slf4jApi, liftJson, junit, junitInterface)
  lazy val testServer = Seq(servletApi, finagleCore, finagleHttp)
}