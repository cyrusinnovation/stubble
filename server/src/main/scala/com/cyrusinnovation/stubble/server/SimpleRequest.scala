package com.cyrusinnovation.stubble.server

import org.jboss.netty.handler.codec.http.{CookieEncoder, HttpVersion, DefaultHttpRequest, HttpMethod}
import org.jboss.netty.buffer.ChannelBuffers._
import org.jboss.netty.util.CharsetUtil._

object SimpleRequest {
  def apply(method: HttpMethod, path: String, body: Option[String] = None, headers: List[(String, String)] = List(), cookies: List[(String, String)] = List()) = {
    val request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, method, path)
    body.map(body => request.setContent(copiedBuffer(body, UTF_8)))
    headers.map {
                  case (name, value) => request.addHeader(name, value)
                }
    val encoder = new CookieEncoder(false)
    cookies.map {
                  case (name, value) => encoder.addCookie(name, value)
                }
    if (cookies.nonEmpty) {
      request.addHeader("Cookie", encoder.encode())
    }
    request
  }
}
