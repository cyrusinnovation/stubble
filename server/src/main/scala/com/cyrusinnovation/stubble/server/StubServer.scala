package com.cyrusinnovation.stubble.server

import net.liftweb.json.{Serialization, DefaultFormats, ShortTypeHints}
import com.twitter.finagle.{Filter, Service}
import org.jboss.netty.handler.codec.http._
import com.twitter.util.{Duration, Future}
import java.net.{URI, SocketAddress, InetSocketAddress}
import com.twitter.finagle.builder.{ServerBuilder, Server}
import com.twitter.finagle.http.Http
import java.util.concurrent.TimeUnit
import collection.immutable.Stack
import org.jboss.netty.util.CharsetUtil.UTF_8

object StubServer {
  final val SerializationFormat = new DefaultFormats {
    override val typeHints = ShortTypeHints(classOf[DefaultHttpResponse] :: RequestCondition.Types)
    override val typeHintFieldName = "type"
  }
}


class StubServer(port: Int) extends StubServerControl {
  var interactionContexts = Stack[List[Interaction]](List())
  val handleControlRequests = new ControlRequestFilter
  val serviceInteractions = new InteractionsService
  val address: SocketAddress = new InetSocketAddress(port)
  var server: Server = _
  implicit val formats = StubServer.SerializationFormat

  class ControlRequestFilter extends Filter[HttpRequest, HttpResponse, HttpRequest, HttpResponse] {
    def apply(request: HttpRequest, service: Service[HttpRequest, HttpResponse]) = {
      if (request.getHeader("X-StubServer-Control") != null) Future(handleInternal(request))
      else service(request)
    }

    def handleInternal(request: HttpRequest): HttpResponse = {
      val SuccessResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)
      val body = request.getContent.toString(UTF_8)
      URI.create(request.getUri).getPath match {
        case "/add-interaction" => addInteraction(Serialization.read[Interaction](body)); SuccessResponse
      }
      new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND)
    }
  }

  class InteractionsService extends Service[HttpRequest, HttpResponse] {
    def apply(request: HttpRequest) = {
      val responses = for {
        context <- interactionContexts
        interaction <- context
        response <- interaction.matchRequest(request)
      } yield response
      Future(responses.headOption.getOrElse(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND)))
    }
  }


  def start() {
    server = ServerBuilder().codec(Http()).bindTo(address).name("StubServer").build(handleControlRequests andThen serviceInteractions)
  }

  def stop() {
    server.close(Duration(10, TimeUnit.SECONDS))
  }

  def addInteraction(interaction: Interaction) {
    interactionContexts = interactionContexts.tail.push(interaction :: interactionContexts.top)
  }

  def popInteractions() {
    interactionContexts = interactionContexts.pop
  }

  def pushInteractions() {
    interactionContexts = interactionContexts.push(List())
  }

  def listInteractions(): List[Interaction] = {
    val interactions = for {
      context <- interactionContexts
      interaction <- context
    } yield interaction
    interactions.toList
  }
}