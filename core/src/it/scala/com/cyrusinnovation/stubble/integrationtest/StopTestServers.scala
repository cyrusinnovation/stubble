package com.cyrusinnovation.stubble.integrationtest

import com.cyrusinnovation.stubble.server.{StubbleClient, StubServer}

object StopTestServers {
  def main(args: Array[String]) {
    val stubble = new StubbleClient(8082)
    stubble.stopServer()
  }
}
