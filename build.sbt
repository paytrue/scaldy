import scalariform.formatter.preferences._

val scalaCompiler = "org.scala-lang" % "scala-compiler" % "2.11.+"

lazy val root = (project in file(".")).
  settings(
    name := "scaldy",
    organization := "com.paytrue",
    version := "0.1",
    scalaVersion := "2.11.+",
    libraryDependencies += scalaCompiler
  )

scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(RewriteArrowSymbols, true)
  .setPreference(AlignParameters, true)
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(PlaceScaladocAsterisksBeneathSecondAsterisk, true)
  .setPreference(MultilineScaladocCommentsStartOnFirstLine, true)
