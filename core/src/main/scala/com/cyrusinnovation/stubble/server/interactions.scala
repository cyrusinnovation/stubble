package com.cyrusinnovation.stubble.server

import org.jboss.netty.handler.codec.http._
import java.net.URI
import org.jboss.netty.buffer.ChannelBuffers._
import org.jboss.netty.util.CharsetUtil._
import scala.collection.JavaConverters._

sealed trait RequestCondition {
  def matches(request: HttpRequest): Boolean
}

case class PathCondition(path: String) extends RequestCondition {
  def matches(request: HttpRequest) = path == URI.create(request.getUri).getPath
}

case class HeaderCondition(header: (String, String)) extends RequestCondition {
  def matches(request: HttpRequest) = header match {
    case (name, value) => Option(request.getHeader(name)).map(_ == value).getOrElse(false)
  }
}

case class CookieCondition(cookie: (String, String)) extends RequestCondition {
  def matches(request: HttpRequest) = cookie match {
    case (name, value) => {
      val headerOption = Option(request.getHeader("Cookie"))
      // DefaultCookie.equals is broken https://github.com/netty/netty/issues/378
      val cookieTuplesOption = headerOption.map(header => cookieTuplesFromHeader(header))
      cookieTuplesOption.map(_.contains((name, value))).getOrElse(false)
    }
  }

  private def cookieTuplesFromHeader(header: String): scala.collection.mutable.Set[(String,String)] = {
    val decoder = new CookieDecoder()
    decoder.decode(header).asScala.map(c => (c.getName, c.getValue))
  }
}

case class RequestParamCondition(name: String, value: Option[String]) extends RequestCondition {
  def matches(request: HttpRequest) = {
    val decoder = new QueryStringDecoder(request.getUri)
    val params = decoder.getParameters.asScala
    val result = for {
      foundValue <- params.get(name)
      expectedValue <- value
    } yield foundValue.contains(expectedValue)
    result.getOrElse(false)
  }
}


case class Response(status: HttpResponseStatus = HttpResponseStatus.OK, content: Option[String], headers: Map[String, String] = Map()) {
  def toHttpResponse = {
    val response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status)
    content.map(content => response.setContent(copiedBuffer(content, UTF_8)))
    headers.foreach {
      case (key, value) => response.addHeader(key, value)
    }
    response
  }
}

case class Interaction(conditions: List[RequestCondition], response: Response) {
  def matchRequest(request: HttpRequest) = {
    if (conditions.forall(_.matches(request))) Some(response.toHttpResponse) else None
  }
}

object RequestCondition {
  final val Types = List[Class[_]](classOf[PathCondition], classOf[CookieCondition], classOf[HeaderCondition])
}

