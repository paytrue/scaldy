package com.paytrue.scaldy

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import scala.io.Source
import scala.reflect.runtime.currentMirror
import scala.reflect.runtime.universe.Flag._
import scala.reflect.runtime.universe._
import scala.tools.reflect.ToolBox

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
      case ident @ Ident(identName) if ident.isType ⇒
        refTypes += identName.toString
      case _ ⇒
    }
    super.traverse(tree)
  }
}

object FileClassFinder {
  def getClassesFromFile(file: Path): List[BeanClass] = {
    val toolbox = currentMirror.mkToolBox()
    val fileContents = Source.fromFile(file.toString, StandardCharsets.UTF_8.name()).getLines().drop(1).mkString("\n")
    val tree = toolbox.parse(fileContents)
    val traverser = new ClassDefTraverser(file)
    traverser.traverse(tree)
    traverser.classes
  }
}