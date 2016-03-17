package com.paytrue.scaldy

import java.nio.file.Path

case class Property(name: String, propType: String, depTypes: Set[String], isRequired: Boolean)

case class BeanClass(name: String, properties: List[Property], parents: List[String], isTrait: Boolean, isAbstract: Boolean, sourceFile: Path) {
  private val stereotype = if (isTrait) """<tr><td colspan="3">«Trait»</td></tr>""" else ""
  private val title = if (isAbstract) s"<i>$name</i>" else s"<b>$name</b>"
  private val borderWidth = if (isTrait || isAbstract) 1 else 2

  def relationships: List[Relationship] = {
    parents.map(Generalization(name, _)) ++
      properties.flatMap {
        case Property(propName, propType, depTypes, _) if depTypes.nonEmpty ⇒
          depTypes.map(Association(name, _, propName, multiple = true)).toList
        case Property(propName, propType, _, _) ⇒
          List(Association(name, propType, propName, multiple = false))
      }
  }

  def node(color: Color): String = s"""$name [label=${label(color)}]"""

  private def label(color: Color) = {
    val backgroundColor = (if (isTrait || isAbstract) color.whiter else color).hexString
    val borderColor = color.darker.hexString

    s"""<
       |<table border="$borderWidth" cellspacing="0" cellborder="0" cellpadding="8" valign="middle" bgcolor="$backgroundColor" color="$borderColor" style="rounded">
       |<tr><td>
       |<table border="0" cellspacing="7" cellpadding="0" valign="middle">
       |$stereotype
       |<tr><td colspan="3"><font point-size="15">$title</font></td></tr>
       |<tr><td colspan="3"></td></tr>
       |<hr/>
       |<tr><td colspan="3"></td></tr>
       |${properties.map(attribute)}
       |</table>
       |</td></tr>
       |</table>
       |>""".stripMargin
  }

  private def attribute(prop: Property) = s"""<tr><td align="left">${attributeName(prop)}:</td><td width="2"></td><td align="left"><font color="#0050b0">${prop.propType}</font></td></tr>"""

  private def attributeName(prop: Property) = if (prop.isRequired) s"<b>${prop.name}</b>" else prop.name
}

trait Relationship {
  def from: String
  def to: String
  def edge: String
}

case class Generalization(from: String, to: String) extends Relationship {
  override def edge = s"$to -> $from [dir=back arrowtail=empty weight=2]"
}

case class Association(from: String, to: String, name: String, multiple: Boolean) extends Relationship {
  override def edge = s"$to -> $from [dir=both arrowtail=open arrowhead=odiamond label=$edgeLabel taillabel=$tailLabel labeldistance=3]"
  private def tailLabel = if (multiple) "N" else "1"
  private def edgeLabel = name
  // private def edgeLabel = s"""<<table border="0" cellborder="0" cellpadding="5" cellspacing="0" style="rounded"><tr><td>$name</td></tr></table>>"""
}