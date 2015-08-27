import scalariform.formatter.preferences._

lazy val root = (project in file(".")).
  settings(
    name := "scaldy",
    organization := "com.paytrue",
    version := "0.1",
    scalaVersion := "2.11.7",
    artifactPath in Compile in packageBin <<=
      baseDirectory { base => base / "release" / "com.paytrue.scaldy.jar" },
    libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value
  )

scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(RewriteArrowSymbols, true)
  .setPreference(AlignParameters, true)
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(PlaceScaladocAsterisksBeneathSecondAsterisk, true)
  .setPreference(MultilineScaladocCommentsStartOnFirstLine, true)
