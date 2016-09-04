name := "unzimm"

scalaVersion := "2.11.8"

resolvers ++= Seq(
  Resolver.bintrayRepo("stanch", "maven"),
  Resolver.bintrayRepo("drdozer", "maven"),
  "justwrote" at "http://repo.justwrote.it/releases/"
)

libraryDependencies ++= Seq(
  "org.stanch" %% "reftree" % "0.3.1",
  "org.stanch" %% "zipper" % "0.2.0",
  "com.github.julien-truffaut" %% "monocle-macro" % "1.2.2",
  "com.softwaremill.quicklens" %% "quicklens" % "1.4.7",
  "it.justwrote" %% "scala-faker" % "0.3",
  "org.scalacheck" %% "scalacheck" % "1.12.5",
  "org.scalaz" %% "scalaz-scalacheck-binding" % "7.2.5",
  "com.thoughtworks.each" %% "each" % "0.6.0",
  "com.chuusai" %% "shapeless" % "2.2.5",
  "org.scalatest" %% "scalatest" % "3.0.0" % Test,
  "com.lihaoyi" % "ammonite" % "0.7.6" % Test cross CrossVersion.full
)

val predef = Seq(
  "import reftree._, zipper._, unzimm._",
  "import Data._, Generators._, LensDiagrams._",
  "import com.softwaremill.quicklens._, monocle.Lens, monocle.macros.GenLens",
  "import scala.collection.immutable._",
  "import java.nio.file.Paths",
  "val defaultDiagram = Diagram(); import defaultDiagram.show"
).mkString(";")

initialCommands in (Test, console) := s"""ammonite.Main("$predef").run(); System.exit(0)"""

addCommandAlias("amm", "test:console")

tutSettings

tutTargetDirectory := baseDirectory.value
