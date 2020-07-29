import sbtassembly.AssemblyPlugin.autoImport.assemblyMergeStrategy

name := "serverless-tapir"

val TapirVersion = "0.16.1"

lazy val root = project
  .in(file("."))
  .aggregate(lambda, deployment)

lazy val lambda = project
  .in(file("lambda"))
  .settings(
    scalaVersion := "2.13.1",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-core" % TapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % TapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % TapirVersion,
      "com.amazonaws" % "aws-lambda-java-events" % "3.1.0"
    )
  )
  .settings(
    assemblyJarName in assembly := "assembly.jar",
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", _@_*) => MergeStrategy.discard
      case PathList(ps@_*) if ps.last endsWith "reference-overrides.conf" => MergeStrategy.concat
      case PathList(ps@_*) if ps.last endsWith "module-info.class" => MergeStrategy.discard
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    }
  )

lazy val deployment = project
  .in(file("deployment"))
  .settings(
    scalaVersion := "2.13.1",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs" % TapirVersion,
      "software.amazon.awscdk" % "lambda" % "1.54.0",
      "software.amazon.awscdk" % "apigatewayv2" % "1.54.0"
    )
  )
  .dependsOn(lambda)

addCommandAlias("assembly", "lambda/assembly")
