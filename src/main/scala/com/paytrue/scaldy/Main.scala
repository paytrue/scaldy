package com.paytrue.scaldy

import java.io._
import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path, Paths }
import scala.collection.JavaConversions._

case class Config(inputPath: String = ".", outputPath: File = new File("scaldy.dot"), groupSelector: String = "") {
  // allows to generate only one subgraph by specifying the name of a class in the subgraph, see def subGraph
  val selectGroup = groupSelector != ""
}

object Main extends App {
  val parser = new scopt.OptionParser[Config]("scaldy") {
    head("scaldy", "0.1.x")
    opt[String]('i', "in") valueName "<directory>" action {
      (x, c) ⇒ c.copy(inputPath = x)
    } text "in is an optional input directory path, by default the current directory"
    opt[File]('o', "out") valueName "<file>" action {
      (x, c) ⇒ c.copy(outputPath = x)
    } text "out is an optional output file, scaldy.dot by default"
    opt[String]('g', "group") valueName "<class name>" action {
      (x, c) ⇒ c.copy(groupSelector = x)
    } text "group is an optional subgraph selector, name one class within the group to generate only its subgraph"
  }

  parser.parse(args, Config()) match {
    case Some(config) ⇒
      val output = exportGraph(config)
      val charOutput: OutputStreamWriter = new OutputStreamWriter(
        new FileOutputStream(
          config.outputPath
        ),
        StandardCharsets.UTF_8
      )

      print(output)
      charOutput.write(output)
      charOutput.close()

    case None ⇒ // bad arguments, error already printed
  }

  def exportGraph(c: Config) = {
    val sourceFiles = FileFinder.listFiles(Paths.get(c.inputPath), ".scala")
    val allClasses = sourceFiles.flatMap(FileClassFinder.getClassesFromFile).filterNot(_.name == "Validated")
    val allNames = allClasses.map(_.name)
    val allRelationships = allClasses.flatMap(_.relationships).filter(rel ⇒ allNames.contains(rel.to))
    val allConnectedClasses =
      allClasses.filter(c ⇒ allRelationships.exists(rel ⇒ rel.from == c.name || rel.to == c.name || c.properties.nonEmpty))
        .groupBy(_.sourceFile)
        .zip(GraphColors.cycledColors)
        .map { case ((file, clazz), color) ⇒ (file, clazz, color) }

    def subGraph(sourceFile: Path, classes: Traversable[BeanClass], color: Color) = {
      val classList = classes.toList
      if (!c.selectGroup || classList.map(_.name).contains(c.groupSelector)) {
        val (innerRels, outerRels) = classList.flatMap(_.relationships).filter(rel ⇒ allNames.contains(rel.to)).partition(rel ⇒ classList.map(_.name).contains(rel.to))
        s"""subgraph "cluster_${sourceFile.toString}" {
           |style=invis
           |margin=30
           |${classes.map(_.node(color)).mkString("\n")}
           |${innerRels.map(_.edge).mkString("\n")}
           |}
           |${outerRels.map(_.edge).mkString("\n")}
           |""".stripMargin
      } else {
        ""
      }
    }

    s"""digraph "Class diagram" {
       |graph[splines=true dpi=55]
       |node[shape=none width=0 height=0 margin=0 fontname=Verdana fontsize=14]
       |edge[fontname=Verdana fontsize=12 arrowsize=1.5 minlen=2.5]
       |
       |${allConnectedClasses.map((subGraph _).tupled).mkString("\n")}
         |}
      |""".
      stripMargin
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
