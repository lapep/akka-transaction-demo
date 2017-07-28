name := "transactions-demo"

version := "1.0"

scalaVersion := "2.12.2"

val akkaV = "10.0.0"
val scalaTestV = "3.0.1"
val circeV = "0.6.1"

libraryDependencies ++= {
  Seq(
    "com.typesafe.akka" %% "akka-http-core" % akkaV,
    "com.typesafe.akka" %% "akka-http" % akkaV,
    "de.heikoseeberger" %% "akka-http-circe" % "1.17.0",

    "io.circe" %% "circe-core" % circeV,
    "io.circe" %% "circe-generic" % circeV,
    "io.circe" %% "circe-parser" % circeV,
    "io.circe" %% "circe-optics" % circeV,

    "org.scalamock" % "scalamock-scalatest-support_2.12" % "3.5.0",
    "org.mockito" % "mockito-core" % "2.7.22",
    "org.scalactic" %% "scalactic" % "3.0.1",
    "org.scalatest" %% "scalatest" % scalaTestV % "test",
    "com.typesafe.akka" %% "akka-http-testkit" % akkaV % "test",
    "com.github.melrief" %% "pureconfig" % "0.5.0"
  )
}
