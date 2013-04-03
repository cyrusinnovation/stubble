package com.cyrusinnovation.stubble.server

import com.twitter.finagle.builder.ClientBuilder
import java.net.InetSocketAddress
import org.jboss.netty.handler.codec.http.HttpMethod
import net.liftweb.json.Serialization
import com.twitter.finagle.ChannelClosedException

class StubbleClient(port: Int) extends StubServerControl {
  val client = ClientBuilder()
  .codec(com.twitter.finagle.http.Http())
  .hosts(new InetSocketAddress(port))
  .hostConnectionLimit(1)
  .build()
  implicit val formats = StubServer.SerializationFormat
  final val ControlHeader = "X-StubServer-Control" -> "true"


  def addInteraction(interaction: Interaction) {
    val body = Serialization.write[Interaction](interaction)
    val headers = List(ControlHeader, "Content-Length" -> body.getBytes.length.toString)
    client(SimpleRequest(HttpMethod.POST, "/add-interaction", body = Some(body), headers = headers)).get()
  }

  def popInteractions() {
    client(SimpleRequest(HttpMethod.POST, "/pop-interactions", headers = List(ControlHeader))).get()
  }

  def pushInteractions() {
    client(SimpleRequest(HttpMethod.POST, "/push-interactions", headers = List(ControlHeader))).get()
  }

  def stopServer() {
    try {
      client(SimpleRequest(HttpMethod.POST, "/shutdown", headers = List(ControlHeader))).get()
    } catch {
      case e: ChannelClosedException => //expected
    }
  }

  def release() {
    client.close()
  }
}