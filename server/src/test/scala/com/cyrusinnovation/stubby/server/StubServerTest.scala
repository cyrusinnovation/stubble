package com.cyrusinnovation.stubby.server

import org.junit.Assert._
import org.junit.{After, Before, Test}
import java.net.InetSocketAddress
import com.twitter.finagle.builder.ClientBuilder
import org.jboss.netty.handler.codec.http._
import com.twitter.finagle.Service
import org.jboss.netty.buffer.ChannelBuffers._
import org.jboss.netty.util.CharsetUtil.UTF_8
import net.liftweb.json.Serialization

object SimpleRequest {
  def apply(method: HttpMethod, path: String, body: Option[String] = None) = {
    val request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, method, path)
    body.map(body => request.setContent(copiedBuffer(body, UTF_8)))
    request
  }
}

class StubServerTest {
  var server: StubServer = _
  var client: Service[HttpRequest, HttpResponse] = _

  @Before
  def setUp() {
    server = new StubServer(8082)
    server.start()
    client = ClientBuilder()
      .codec(com.twitter.finagle.http.Http())
      .hosts(new InetSocketAddress(8082))
      .hostConnectionLimit(1)
      .build()
  }

  @After
  def tearDown() {
    server.stop()
    client.release()
  }

  @Test
  def respondsWith404WithNoInteractions() {
    val response = client(SimpleRequest(HttpMethod.GET, "/")).get()
    assertEquals(404, response.getStatus.getCode)
  }

  @Test
  def simpleInteractionResponseBodyMatches() {
    val body = "it works!"
    server.addInteraction(Interaction(List(PathCondition("/")), Response(HttpResponseStatus.OK, Some(body))))
    val response = client(SimpleRequest(HttpMethod.GET, "/")).get()
    assertEquals(body, response.getContent.toString(UTF_8))
  }

  @Test
  def serializesInteractions() {
    implicit val formats = StubServer.SerializationFormat

    server.addInteraction(Interaction(List(PathCondition("/")), Response(HttpResponseStatus.OK, Some("hello!"))))
    val json = Serialization.write(server.interactions)
    println(json)
    assertEquals(server.interactions, Serialization.read[List[Interaction]](json))
  }
}