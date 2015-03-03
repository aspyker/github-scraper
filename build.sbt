name := """github-scraper"""

version := "1.0"

scalaVersion := "2.10.3"

fork := true

libraryDependencies ++= {
  val scalaTestVersion = "2.1.6"
  val logbackVersion = "1.1.2"
  val playJsonVersion = "2.4.0-M2"
  val kohsukeGithubVersion = "1.62"
  Seq(
    "org.scalatest" %% "scalatest" % scalaTestVersion % "test",
    "ch.qos.logback" % "logback-classic" % logbackVersion,
    "com.typesafe.play" % "play-json_2.10" % playJsonVersion,
    "org.kohsuke" % "github-api" %   kohsukeGithubVersion
  )
}
