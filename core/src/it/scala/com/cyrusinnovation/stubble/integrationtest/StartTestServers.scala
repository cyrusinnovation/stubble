package com.cyrusinnovation.stubble.integrationtest

import com.cyrusinnovation.stubble.server.StubServer
import com.twitter.logging.{ConsoleHandler, Logger, LoggerFactory}

object StartTestServers {
  def main(args: Array[String]) {
    startStubble()
  }
  def startStubble() {
    LoggerFactory(
      node = "",
      level = Some(Logger.INFO),
      handlers = List(ConsoleHandler())
    )()
    LoggerFactory(
      node = "com.twitter.jvm",
      level = Some(Logger.ERROR),
      handlers = List(ConsoleHandler())
    )()
    val log = Logger(StartTestServers.getClass)
    log.info("Starting StubServer")
    val server = new StubServer(8082)
    server.start()
  }
}
