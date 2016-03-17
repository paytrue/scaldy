import scalariform.formatter.preferences._

val scalaCompiler = "org.scala-lang" % "scala-compiler" % "2.11.+"

lazy val root = (project in file(".")).
  settings(
    name := "scaldy",
    organization := "com.paytrue",
    version := "0.1",
    scalaVersion := "2.11.+",
    artifactPath in Compile in packageBin <<=
      baseDirectory { base => base / "release" / "com.paytrue.scaldy.jar" },
    libraryDependencies ++= Seq(scalaCompiler, "com.github.scopt" % "scopt_2.11" % "3+")
  )

scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(RewriteArrowSymbols, true)
  .setPreference(AlignParameters, true)
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(PlaceScaladocAsterisksBeneathSecondAsterisk, true)
  .setPreference(MultilineScaladocCommentsStartOnFirstLine, true)
