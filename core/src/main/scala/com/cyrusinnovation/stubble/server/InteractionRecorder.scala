package com.cyrusinnovation.stubble.server

import com.twitter.finagle.{Filter, Service}
import org.jboss.netty.handler.codec.http._
import com.twitter.conversions.time._
import com.twitter.finagle.builder.{ServerBuilder, ClientBuilder}
import com.twitter.finagle.http.Http
import org.jboss.netty.util.CharsetUtil.UTF_8
import java.net.{InetSocketAddress, SocketAddress}
import com.twitter.util.Future

object InteractionRecorder {
  def main(args: Array[String]) {
    new InteractionRecorder(8080).start()
  }
}

class InteractionRecorder(port: Int) {
  val address: SocketAddress = new InetSocketAddress(port)

  def start() {
    val server = ServerBuilder()
      .codec(Http())
      .bindTo(address)
      .name("InteractionRecorder")
      .build(new RecordInteraction andThen new TransparentProxy)

  }

  class TransparentProxy extends Service[HttpRequest, HttpResponse] {
    def apply(request: HttpRequest): Future[HttpResponse] = {
      val client = ClientBuilder()
        .codec(Http())
        .hosts(hostAddressFromHeader(request))
        .hostConnectionLimit(1)
        .tcpConnectTimeout(2.seconds)
        .build()
      client(request) ensure {
        client.release()
      }
    }

    private def hostAddressFromHeader(request: HttpRequest): InetSocketAddress = {
      val HostPort = """([^:]+):?(\d*)""".r
      request.getHeader("Host") match {
        case HostPort(host, port) if port != "" => new InetSocketAddress(host, port.toInt)
        case _@host => new InetSocketAddress(host, 80)
      }
    }
  }

  class RecordInteraction extends Filter[HttpRequest, HttpResponse, HttpRequest, HttpResponse] {
    def apply(request: HttpRequest, service: Service[HttpRequest, HttpResponse]) = {
      println(request)
      service(request) onSuccess {
        response =>
          println(response)
          println(response.getContent.toString(UTF_8))
      }
    }
  }
}
