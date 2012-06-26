package com.cyrusinnovation.stubble.server

trait StubServerControl {
  def addInteraction(interaction: Interaction)
  def popInteractions()
  def pushInteractions()
  def stopServer()
}