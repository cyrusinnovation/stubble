package com.cyrusinnovation.stubble.server

import org.junit.{After, Before, Test}
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import org.junit.Assert._
import scala.Some

class StubServerClientTest {
  var server: StubServer = _
  var client: StubServerClient = _
  final val TestPort = 8082

  @Before
  def setUp() {
    server = new StubServer(TestPort)
    server.start()
    client = new StubServerClient(TestPort)
  }

  @After
  def tearDown() {
    server.stop()
    client.release()
  }

  @Test
  def addsInteractionsToServer() {
    val interactions = List(Interaction(List(CookieCondition("type" -> "chocolate chip")), Response(HttpResponseStatus.OK, Some("gimme cookie!"))),
                            Interaction(List(PathCondition("/")), Response(HttpResponseStatus.OK, Some("Hello!"))))
    interactions.foreach(client.addInteraction(_))
    assertEquals(interactions.reverse, server.listInteractions())
  }
}