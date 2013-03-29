package com.cyrusinnovation.stubble.integrationtest

import org.junit.Assert._
import org.junit.{After, Before, Test}
import com.cyrusinnovation.stubble.server.StubbleClient
import org.jboss.netty.handler.codec.http._
import com.twitter.finagle.builder.ClientBuilder
import java.net.InetSocketAddress
import org.jboss.netty.util.CharsetUtil.UTF_8
import com.cyrusinnovation.stubble.server.Interaction
import com.cyrusinnovation.stubble.server.Response
import com.cyrusinnovation.stubble.server.PathCondition

class EndToEndTest {
  final val StubblePort = System.getProperty("stubblePort", "8082").toInt
  final val ServerPort = System.getProperty("serverPort", "8081").toInt
  var stubble: StubbleClient = _

  @Before
  def setUp() {
    stubble = new StubbleClient(StubblePort)
    stubble.pushInteractions()
  }

  @After
  def tearDown() {
    stubble.popInteractions()
    stubble.release()
  }

  @Test
  def forwardsContentFromBackend() {
    val serverResponseBody = "It works!"
    stubble.addInteraction(Interaction(List(PathCondition("/")), Response(HttpResponseStatus.OK, Some(serverResponseBody))))
    val client = ClientBuilder()
      .codec(com.twitter.finagle.http.Http())
      .hosts(new InetSocketAddress(ServerPort))
      .hostConnectionLimit(1)
      .build()

    val request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/")
    request.setHeader(HttpHeaders.Names.HOST, "localhost:" + ServerPort)
//    val response = client(request).get()
//    assertEquals(serverResponseBody, response.getContent.toString(UTF_8).trim)
  }
}
