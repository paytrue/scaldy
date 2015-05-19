package com.paytrue.scaldy

import java.io._
import java.nio.charset.{StandardCharsets, Charset}
import java.nio.file.{Files, Path, Paths}

import scala.collection.JavaConversions._
import scala.io.Source
import scala.reflect.runtime.currentMirror
import scala.reflect.runtime.universe.Flag._
import scala.reflect.runtime.universe._
import scala.tools.reflect.ToolBox

object Main extends App {
  val inputPath = if (args.length >= 1) args(0) else "."
  val outputPath = if (args.length >= 2) args(1) else "scaldy.dot"

  val sourceFiles = FileFinder.listFiles(Paths.get(inputPath), ".scala")
  val nodeColors = List(
    Color(210, 210, 255),
    Color(255, 230, 170),
    Color(255, 210, 210),
    Color(200, 255, 190),
    Color(190, 230, 255),
    Color(235, 235, 200)
  )
  val allClasses = sourceFiles.flatMap(getClassesFromFile).filterNot(_.name == "Validated")
  val allNames = allClasses.map(_.name)
  val allRelationships = allClasses.flatMap(_.relationships).filter(rel ⇒ allNames.contains(rel.to))
  val allConnectedClasses =
    allClasses.filter(c ⇒ allRelationships.exists(rel ⇒ rel.from == c.name || rel.to == c.name || c.properties.nonEmpty))
      .groupBy(_.sourceFile)
      .zip(cycledColors)
      .map { case ((file, clazz), color) ⇒ (file, clazz, color) }
  val output =
    s"""digraph "Class diagram" {
       |graph[splines=true dpi=55]
       |node[shape=none width=0 height=0 margin=0 fontname=Verdana fontsize=14]
       |edge[fontname=Verdana fontsize=12 arrowsize=1.5 minlen=2.5]
       |
       |${allConnectedClasses.map((subGraph _).tupled).mkString("\n")}
      |}
      |""".stripMargin
  val charOutput: OutputStreamWriter = new OutputStreamWriter(
    new FileOutputStream(outputPath),
    StandardCharsets.UTF_8
  )

  def getClassesFromFile(file: Path): List[BeanClass] = {
    val toolbox = currentMirror.mkToolBox()
    val fileContents = Source.fromFile(file.toString, StandardCharsets.UTF_8.name()).getLines().drop(1).mkString("\n")
    val tree = toolbox.parse(fileContents)
    val traverser = new ClassDefTraverser(file)
    traverser.traverse(tree)
    traverser.classes
  }

  def cycledColors = Stream.continually(nodeColors.toStream).flatten

  print(output)

  def subGraph(sourceFile: Path, classes: Traversable[BeanClass], color: Color) = {
    val classList = classes.toList
    val (innerRels, outerRels) = classList.flatMap(_.relationships).filter(rel ⇒ allNames.contains(rel.to)).partition(rel ⇒ classList.map(_.name).contains(rel.to))

    s"""subgraph "cluster_${sourceFile.toString}" {
                                                  |style=invis
                                                  |margin=30
                                                  |${classes.map(_.node(color)).mkString("\n")}
        |${innerRels.map(_.edge).mkString("\n")}
        |}
        |${outerRels.map(_.edge).mkString("\n")}
        |""".stripMargin
  }
  charOutput.write(output)
  charOutput.close()
}

case class Color(red: Int, green: Int, blue: Int) {
  def hexString = "#%02x%02x%02x".format(red, green, blue)
  def whiter = Color(whiten(red), whiten(green), whiten(blue))

  private def whiten(component: Int) = (component + 255) / 2

  def darker = Color(darken(red), darken(green), darken(blue))

  private def darken(component: Int) = component / 2
}

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

class ClassDefTraverser(file: Path) extends Traverser {
  var classes: List[BeanClass] = List.empty

  override def traverse(tree: Tree) = {
    tree match {
      case ClassDef(mods, name, _, impl) ⇒
        val valTraverser = new ValDefTraverser
        valTraverser.traverse(tree)
        val parents = impl.parents.map(_.toString())
        classes = classes :+ BeanClass(name.toString, valTraverser.properties, parents,
          isAbstract = mods.hasFlag(ABSTRACT), isTrait = mods.hasFlag(TRAIT), sourceFile = file)

      case _ ⇒
    }
    super.traverse(tree)
  }
}

class ValDefTraverser extends Traverser {
  var properties: List[Property] = List.empty

  override def traverse(tree: Tree) = {
    tree match {
      case ValDef(Modifiers(_, _, annotations), valName, tpt, _) if hasBeanProperty(annotations) ⇒
        val isRequired = hasRequired(annotations)
        tpt match {
          case AppliedTypeTree(Select(qualifier, typeName), args) ⇒
            val typeTraverser = new TypeArgsTraverser
            typeTraverser.traverseTrees(args)
            properties :+= Property(valName.toString, tpt.toString(), typeTraverser.refTypes, isRequired)

          case _ ⇒
            properties :+= Property(valName.toString, tpt.toString(), Set.empty, isRequired)
        }

      case _ ⇒
    }
    super.traverse(tree)
  }

  private def hasBeanProperty(annotations: List[Tree]) = annotations.exists {
    case Apply(Select(New(Ident(TypeName("BeanProperty"))), _), _) ⇒ true
    case _ ⇒ false
  }

  private def hasRequired(annotations: List[Tree]) = annotations.exists {
    case Apply(Select(New(Annotated(_, Ident(TypeName("Required")))), _), _) ⇒ true
    case _ ⇒ false
  }
}

class TypeArgsTraverser extends Traverser {
  var refTypes: Set[String] = Set.empty

  override def traverse(tree: Tree) = {
    tree match {
      case ident@Ident(identName) if ident.isType ⇒
        refTypes += identName.toString
      case _ ⇒
    }
    super.traverse(tree)
  }
}

object FileFinder {
  def listFiles(root: Path, fileSuffix: String): List[Path] = {
    var files = List.empty[Path]

    Files.newDirectoryStream(root).foreach(path ⇒ {
      if (Files.isDirectory(path)) {
        files = files ++ listFiles(path, fileSuffix)
      } else if (path.toString.endsWith(fileSuffix)) {
        files = files ++ List(path.toAbsolutePath)
      }
    })

    files
  }
}
