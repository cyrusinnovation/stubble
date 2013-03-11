package com.cyrusinnovation.stubble.integrationtest

import com.cyrusinnovation.stubble.server.StubServer

object StartTestServers {
  def main(args: Array[String]) {
    val server = new StubServer(8082)
    server.start()
  }
  def start() {
    val server = new StubServer(8082)
    server.start()
  }

  def stop() {

  }
}
