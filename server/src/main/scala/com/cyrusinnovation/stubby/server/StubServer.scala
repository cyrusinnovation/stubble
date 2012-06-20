package com.cyrusinnovation.stubby.server

import com.twitter.finagle.Service
import org.jboss.netty.handler.codec.http._
import com.twitter.util.{Duration, Future}
import java.net.{URI, SocketAddress, InetSocketAddress}
import com.twitter.finagle.builder.{ServerBuilder, Server}
import com.twitter.finagle.http.Http
import java.util.concurrent.TimeUnit

sealed trait RequestCondition {
  def matches(request:HttpRequest): Boolean
}
case class PathCondition(path: String) extends RequestCondition {
  def matches(request: HttpRequest) = path == URI.create(request.getUri).getPath
}
case class Interaction(conditions: List[RequestCondition], response: HttpResponse) {
  def matchRequest(request: HttpRequest) = {
    if (conditions.forall(_.matches(request))) Some(response) else None
  }
}

class StubServer(port: Int) {
  var interactions = List[Interaction]()
  def addInteraction(interaction: Interaction) {
    interactions = interaction :: interactions
  }

  val service: Service[HttpRequest,HttpResponse] = new Service[HttpRequest,HttpResponse] {
    def apply(request: HttpRequest) = Future(interactions.flatMap(_.matchRequest(request)).headOption.getOrElse(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND)))
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