name := "PeLoTi"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  anorm,
  cache,
  "org.reactivemongo" %% "play2-reactivemongo" % "0.10.2",
  "org.mindrot" % "jbcrypt" % "0.3m",
  "play.modules.mailer" %% "play-mailer" % "2.1.3",
  "nl.rhinofly" %% "jira-exception-processor" % "3.1.4"
)

resolvers ++= Seq(
  "Rhinofly Internal Release Repository" at "http://maven-repository.rhinofly.net:8081/artifactory/libs-release-local",
  "Sonatype OSS Releases" at "http://oss.sonatype.org/content/repositories/releases")     

playScalaSettings

templatesImport += "models.requests._"

scalacOptions += "-feature"