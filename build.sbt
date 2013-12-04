name := "PeLoTi"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  "com.novus" % "salat_2.10" % "1.9.2",
  "se.radley" % "play-plugins-salat_2.10" % "1.3.0"
)     

playScalaSettings