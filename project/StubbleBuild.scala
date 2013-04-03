import com.github.siasia.{WebPlugin, Container, PluginKeys}
import sbt._
import Keys._
import sbt.Tests.{Setup, Cleanup}

object StubbleBuild extends Build {
  lazy val buildSettings = Seq(
    organization := "com.cyrusinnovation",
    name := "stubble",
    version := "0.0.3-SNAPSHOT",
    scalaVersion := "2.9.2",
    crossScalaVersions := Seq("2.9.2", "2.10.0"),
    resolvers ++= Seq("twttr" at "http://maven.twttr.com/"),
    parallelExecution in Test := false,
    scalacOptions += "-deprecation"
    //    dependencyOverrides ++= Dependencies.core
  )
  override lazy val settings = super.settings ++ buildSettings

  lazy val root = Project(id = "stubble",
                          base = file("."),
                          settings = Project.defaultSettings)
    //    .configs(IntegrationTest)
    //    .settings(libraryDependencies ++= Dependencies.allJunit)
    //    .settings(Defaults.itSettings: _*)
    .aggregate(core)
  lazy val core = Project(id = "core",
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
  //  .settings(testOptions in IntegrationTest += Tests.Setup(loader: ClassLoader => {
  //  val clazz = loader.loadClass("com.cyrusinnovation.stubble.server.StubServer")
  //  val constructor = clazz.getConstructor(classOf[Int])
  //  val server: StubServer = constructor.newInstance(8082)
  //  server.start()
  //  }))

  //  lazy val specs = "org.scala-tools.testing" %% "specs" % "1.6.8" % "it,test"
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