package com.cyrusinnovation.stubby.server

import org.junit.Assert._
import org.junit.Test
import dispatch.{url, Http}

class StubServerTest {
  @Test
  def respondsToSpecifiedPort() {
    val server = new StubServer(8082)
    server.start()

    val http = new Http
    val response = http x (url("http://localhost:8082/") as_str)
    assertEquals("", response)
  }
}