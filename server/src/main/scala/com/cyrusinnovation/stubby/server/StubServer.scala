package com.cyrusinnovation.stubby.server

import com.twitter.finagle.Service
import org.jboss.netty.handler.codec.http._
import com.twitter.util.{Duration, Future}
import java.net.{SocketAddress, InetSocketAddress}
import com.twitter.finagle.builder.{ServerBuilder, Server}
import com.twitter.finagle.http.Http
import java.util.concurrent.TimeUnit

class StubServer(port: Int) {
  val service: Service[HttpRequest,HttpResponse] = new Service[HttpRequest,HttpResponse] {
    def apply(request: HttpRequest) = Future(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK))
  }
  val address: SocketAddress = new InetSocketAddress(port)
  var server: Server = _

  def start() {
    server = ServerBuilder().codec(Http()).bindTo(address).name("StubServer").build(service)
  }

  def stop() {
    server.close(Duration(10, TimeUnit.SECONDS))
  }

}