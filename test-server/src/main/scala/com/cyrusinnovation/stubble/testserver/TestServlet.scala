package com.cyrusinnovation.stubble.testserver

import javax.servlet.http.{HttpServletResponse, HttpServletRequest, HttpServlet}
import com.twitter.finagle.builder.ClientBuilder
import java.net.InetSocketAddress
import org.jboss.netty.handler.codec.http.{HttpMethod, HttpVersion, DefaultHttpRequest}
import org.jboss.netty.util.CharsetUtil.UTF_8

class TestServlet extends HttpServlet {
  val client = ClientBuilder()
    .codec(com.twitter.finagle.http.Http())
    .hosts(new InetSocketAddress(System.getProperty("stubblePort", "8082").toInt))
    .hostConnectionLimit(1)
    .build()

  override def doGet(request: HttpServletRequest, response: HttpServletResponse) {
    val backendResponse = client(new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/"))
    backendResponse onFailure (e => println("TEST SERVER: back end request failed: " + e.getMessage))
    response.getWriter.println(backendResponse.get().getContent.toString(UTF_8))
  }
}
