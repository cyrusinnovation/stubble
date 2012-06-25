package com.cyrusinnovation.stubble.server.example

import com.cyrusinnovation.stubble.server._
import org.junit._
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import org.junit.Assert._
import com.cyrusinnovation.stubble.server.Interaction
import com.cyrusinnovation.stubble.server.CookieCondition
import com.cyrusinnovation.stubble.server.Response
import com.cyrusinnovation.stubble.server.PathCondition

class ExampleApplication {

  import com.twitter.finagle.builder.ClientBuilder
  import java.net.InetSocketAddress
  import org.jboss.netty.handler.codec.http.HttpMethod
  import org.jboss.netty.util.CharsetUtil.UTF_8

  def somethingThatNeedsABackendServer = {
    val client = ClientBuilder()
      .codec(com.twitter.finagle.http.Http())
      .hosts(new InetSocketAddress(8082))
      .hostConnectionLimit(1)
      .build()

    val response = client(SimpleRequest(HttpMethod.GET, "/")).get()
    if (response.getContent.toString(UTF_8).contains("Hello")) "Something wonderful happened"
    else "Oops"
  }
}

object ExampleTest {
  var server: StubServer = _

  @BeforeClass
  def exampleSetupThatWouldNormallyHappenInTheBuild() {
    server = new StubServer(8082)
    server.start()
  }

  @AfterClass
  def exampleCleanupThatWouldNormallyHappenInTheBuild() {
    server.stop()
  }
}

class ExampleTest {
  var client: StubbleClient = new StubbleClient(8082)
  val app = new ExampleApplication

  @Before
  def setUp() {
    client.pushInteractions()
  }

  @After
  def tearDown() {
    client.popInteractions()
  }

  @Test
  def addsInteractionsToServer() {
    val interactions = List(Interaction(List(CookieCondition("type" -> "chocolate chip")), Response(HttpResponseStatus.OK, Some("gimme cookie!"))),
                            Interaction(List(PathCondition("/")), Response(HttpResponseStatus.OK, Some("Hello!"))))
    interactions.foreach(client.addInteraction(_))

    val appResponse = app.somethingThatNeedsABackendServer
    assertEquals("Something wonderful happened", appResponse)
  }
}
