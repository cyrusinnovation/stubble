package com.cyrusinnovation.stubble.server

import org.junit.{After, Before, Test}
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import org.junit.Assert._
import scala.Some

class StubbleClientTest {
  var server: StubServer = _
  var client: StubbleClient = _
  final val TestPort = 8082

  @Before
  def setUp() {
    server = new StubServer(TestPort)
    server.start()
    client = new StubbleClient(TestPort)
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

  @Test
  def pushesAndPopsInteractionsOnServer() {
    client.pushInteractions()
    assertEquals("Initial state is no interactions", List(), server.listInteractions())
    val interaction = Interaction(List(PathCondition("/")), Response(HttpResponseStatus.OK, Some("Hello!")))
    client.addInteraction(interaction)
    assertEquals("Interaction is live", List(interaction), server.listInteractions())
    client.popInteractions()
    assertEquals("Back to initial state", List(), server.listInteractions())
  }

  @Test
  def stopsRunningServer() {
    client.stopServer()
  }
}