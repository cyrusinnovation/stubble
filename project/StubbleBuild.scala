import sbt._
import Keys._

object StubbleBuild extends Build {
  lazy val buildSettings = Seq(
    organization := "com.cyrusinnovation",
    name := "stubble",
    version := "0.0.3-SNAPSHOT",
    scalaVersion := "2.9.2",
    crossScalaVersions := Seq("2.9.2", "2.10.0"),
    resolvers ++= Seq("twttr" at "http://maven.twttr.com/"),
    parallelExecution in Test := false
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
  //  .settings(testOptions in IntegrationTest += Tests.Setup(loader: ClassLoader => {
  //  val clazz = loader.loadClass("com.cyrusinnovation.stubble.server.StubServer")
  //  val constructor = clazz.getConstructor(classOf[Int])
  //  val server: StubServer = constructor.newInstance(8082)
  //  server.start()
  //  }))

  //  lazy val specs = "org.scala-tools.testing" %% "specs" % "1.6.8" % "it,test"
  val startStubble = TaskKey[Unit]("start-stubble", "Starts an instance of the Stubble server")
  val stopStubble = TaskKey[Unit]("stop-stubble", "Stops a running instance of the Stubble server")
}


object Dependencies {
  val slf4jVersion = "1.6.1"
  val finagleVersion = "5.1.0"

  object Compile {
    val jodaTime = "joda-time" % "joda-time" % "1.6.2"
    val logback = "ch.qos.logback" % "logback-classic" % "0.9.25"
    val slf4jApi = "org.slf4j" % "slf4j-api" % slf4jVersion
    val liftJson = "net.liftweb" % "lift-json_2.9.1" % "2.4"
    val finagleCore = "com.twitter" % "finagle-core" % finagleVersion
    val finagleHttp = "com.twitter" % "finagle-http" % finagleVersion
  }

  object Test {
    val junit = "junit" % "junit" % "4.7" % "test,it"
    val junitInterface = "com.novocode" % "junit-interface" % "0.8" % "test,it"
  }

  import Compile._
  import Test._

  lazy val allJunit = Seq(junit, junitInterface)
  lazy val core = Seq(finagleCore, finagleHttp, logback, jodaTime, slf4jApi, liftJson, junit, junitInterface)
}