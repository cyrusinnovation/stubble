package com.cyrusinnovation.stubble.server

import com.twitter.finagle.builder.ClientBuilder
import java.net.InetSocketAddress
import org.jboss.netty.handler.codec.http.HttpMethod
import net.liftweb.json.Serialization

class StubServerClient(port: Int) extends StubServerControl {
  val client = ClientBuilder()
  .codec(com.twitter.finagle.http.Http())
  .hosts(new InetSocketAddress(port))
  .hostConnectionLimit(1)
  .build()
  implicit val formats = StubServer.SerializationFormat


  def addInteraction(interaction: Interaction) {
    val body = Serialization.write[Interaction](interaction)
    val headers = List("X-StubServer-Control" -> "true", "Content-Length" -> body.getBytes.length.toString)
    client(SimpleRequest(HttpMethod.POST, "/add-interaction", body = Some(body), headers = headers)).get()
  }

  def popInteractions() {}

  def pushInteractions() {}
  def release() {
    client.release()
  }
}