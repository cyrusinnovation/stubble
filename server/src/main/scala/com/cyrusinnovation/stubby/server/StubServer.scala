package com.cyrusinnovation.stubby.server

import net.liftweb.json.DefaultFormats
import net.liftweb.json.ShortTypeHints
import com.twitter.finagle.Service
import org.jboss.netty.handler.codec.http._
import com.twitter.util.{Duration, Future}
import java.net.{URI, SocketAddress, InetSocketAddress}
import com.twitter.finagle.builder.{ServerBuilder, Server}
import com.twitter.finagle.http.Http
import java.util.concurrent.TimeUnit
import org.jboss.netty.buffer.ChannelBuffers._
import org.jboss.netty.util.CharsetUtil.UTF_8
import collection.immutable.Stack


sealed trait RequestCondition {
  def matches(request:HttpRequest): Boolean
}
case class PathCondition(path: String) extends RequestCondition {
  def matches(request: HttpRequest) = path == URI.create(request.getUri).getPath
}

case class Response(status: HttpResponseStatus = HttpResponseStatus.OK, content: Option[String], headers: Map[String,String] = Map()) {
  def toHttpResponse = {
    val response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status)
    content.map(content => response.setContent(copiedBuffer(content, UTF_8)))
    headers.foreach { case (key,value) => response.addHeader(key, value) }
    response
  }
}
case class Interaction(conditions: List[RequestCondition], response: Response) {
  def matchRequest(request: HttpRequest) = {
    if (conditions.forall(_.matches(request))) Some(response.toHttpResponse) else None
  }
}
object RequestCondition {
  final val Types = List(classOf[PathCondition])
}

object StubServer {
  final val SerializationFormat = new DefaultFormats {
    override val typeHints = ShortTypeHints(classOf[DefaultHttpResponse] :: RequestCondition.Types)
    override val typeHintFieldName = "type"
  }
}

class StubServer(port: Int) {
  var interactionContexts = Stack[List[Interaction]](List())
  def addInteraction(interaction: Interaction) {
    interactionContexts = interactionContexts.tail.push(interaction :: interactionContexts.top)
  }

  def popInteractions() {
    interactionContexts = interactionContexts.pop
  }

  def pushInteractions() {
    interactionContexts = interactionContexts.push(List())
  }

  val service: Service[HttpRequest,HttpResponse] = new Service[HttpRequest,HttpResponse] {
    def apply(request: HttpRequest) = Future(interactionContexts.top.flatMap(_.matchRequest(request)).headOption.getOrElse(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND)))
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