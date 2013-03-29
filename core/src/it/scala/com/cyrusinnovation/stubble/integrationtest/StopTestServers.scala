package com.cyrusinnovation.stubble.integrationtest

import com.cyrusinnovation.stubble.server.{StubbleClient, StubServer}
import com.twitter.logging.Logger

object StopTestServers {
  def main(args: Array[String]) {
    stopStubble()
  }

  def stopStubble() {
    val log = Logger(StartTestServers.getClass)
    log.info("Stopping StubServer")
    val stubble = new StubbleClient(8082)
    stubble.stopServer()
  }
}
