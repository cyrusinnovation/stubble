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
  def apply(method: HttpMethod, path: String, body: Option[String] = None, headers: List[(String, String)] = List(), cookies: List[(String,String)] = List()) = {
    val request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, method, path)
    body.map(body => request.setContent(copiedBuffer(body, UTF_8)))
    headers.map {
      case (name, value) => request.addHeader(name, value)
    }
    val encoder = new CookieEncoder(false)
    cookies.map {case (name, value) => encoder.addCookie(name, value)}
    if (cookies.nonEmpty) {request.addHeader("Cookie", encoder.encode())}
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

    server.addInteraction(Interaction(List(PathCondition("/"), HeaderCondition("X-Foo" -> "bar"), HeaderCondition("flavor" -> "chocolate")), Response(HttpResponseStatus.OK, Some("hello!"))))
    val json = Serialization.write(server.interactionContexts.top)
    assertEquals(server.interactionContexts.top, Serialization.read[List[Interaction]](json))
  }

  @Test
  def popInteractionsCleansNewInteractionsFromStack() {
    server.pushInteractions()
    assertEquals("Initial state is no interactions", HttpResponseStatus.NOT_FOUND, client(SimpleRequest(HttpMethod.GET, "/")).get().getStatus)
    server.addInteraction(Interaction(List(PathCondition("/")), Response(HttpResponseStatus.OK, Some("Hello!"))))
    assertEquals("Interaction is live", HttpResponseStatus.OK, client(SimpleRequest(HttpMethod.GET, "/")).get().getStatus)
    server.popInteractions()
    assertEquals("Back to initial state", HttpResponseStatus.NOT_FOUND, client(SimpleRequest(HttpMethod.GET, "/")).get().getStatus)
  }

  @Test
  def interactionsStackIsAdditive() {
    val lowerBody = "Step 1"
    val upperBody = "Step 2"
    server.pushInteractions()
    server.addInteraction(Interaction(List(PathCondition("/level1")), Response(HttpResponseStatus.OK, Some(lowerBody))))
    server.pushInteractions()
    server.addInteraction(Interaction(List(PathCondition("/level2")), Response(HttpResponseStatus.OK, Some(upperBody))))
    assertEquals(lowerBody, client(SimpleRequest(HttpMethod.GET, "/level1")).get().getContent.toString(UTF_8))
    assertEquals(upperBody, client(SimpleRequest(HttpMethod.GET, "/level2")).get().getContent.toString(UTF_8))
  }

  @Test
  def matchesOnHeaders() {
    server.addInteraction(Interaction(List(HeaderCondition("X-Foo-Header" -> "bars")), Response(HttpResponseStatus.OK, Some("it works!"))))
    assertEquals("it works!", client(SimpleRequest(HttpMethod.GET, "/foo", headers = List(("X-Foo-Header" -> "bars")))).get().getContent.toString(UTF_8))
  }

  @Test
  def doesNotMatchWrongHeaderValue() {
    server.addInteraction(Interaction(List(HeaderCondition("X-Foo-Header" -> "bars")), Response(HttpResponseStatus.OK, Some("it works!"))))
    assertEquals(HttpResponseStatus.NOT_FOUND, client(SimpleRequest(HttpMethod.GET, "/foo", headers = List(("X-Foo-Header" -> "quxes")))).get().getStatus)
  }

  @Test
  def matchesCookies() {
    server.addInteraction(Interaction(List(CookieCondition("type" -> "chocolate chip")), Response(HttpResponseStatus.OK, Some("gimme cookie!"))))
    assertEquals("gimme cookie!", client(SimpleRequest(HttpMethod.GET, "/allthecookies", cookies = List("type" -> "chocolate chip"))).get().getContent.toString(UTF_8))
  }

  @Test
  def doesNotMatchDifferentCookieValue() {
    server.addInteraction(Interaction(List(CookieCondition("type" -> "chocolate chip")), Response(HttpResponseStatus.OK, Some("gimme cookie!"))))
    assertEquals(HttpResponseStatus.NOT_FOUND, client(SimpleRequest(HttpMethod.GET, "/allthecookies", cookies = List("type" -> "chocolate chip oatmeal"))).get().getStatus)
  }
}