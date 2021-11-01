libraryDependencies += "org.apache.httpcomponents" % "httpcore" % "4.4.12"
libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.5.10"
libraryDependencies += "org.json" % "json" % "20190722"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name := """gha-play""",
    organization := "com.example",
    version := "1.0-SNAPSHOT",
    scalaVersion := "2.12.10",
    libraryDependencies ++= Seq(
      guice,
      "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
    ),
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation",
      "-Xfatal-warnings"
    )
  )
