package com.paytrue.scaldy

object GraphColors {
  val nodeColors = List(
    Color(210, 210, 255),
    Color(255, 230, 170),
    Color(255, 210, 210),
    Color(200, 255, 190),
    Color(190, 230, 255),
    Color(235, 235, 200)
  )

  def cycledColors = Stream.continually(nodeColors.toStream).flatten
}

case class Color(red: Int, green: Int, blue: Int) {
  def hexString = "#%02x%02x%02x".format(red, green, blue)
  def whiter = Color(whiten(red), whiten(green), whiten(blue))

  private def whiten(component: Int) = (component + 255) / 2

  def darker = Color(darken(red), darken(green), darken(blue))

  private def darken(component: Int) = component / 2
}