// Copyright Shunsuke Sogame 2008-2010.
// Distributed under the terms of an MIT-style license.
name := "hano"

version := "0.1.1-SNAPSHOT"

crossScalaVersions := Seq("2.9.0", "2.9.1", "2.9.2", "2.10.0")

libraryDependencies <+= scalaVersion( v =>
  compilerPlugin("org.scala-lang.plugins" % "continuations" % v)
)

scalacOptions += "-P:continuations:enable"

libraryDependencies ++= {
  val fest = "1.2.1"
  Seq(
    "junit" % "junit" % "4.11"
   ,"org.testng" % "testng" % "5.14"
   ,"org.easytesting" % "fest-swing" % fest
   ,"org.easytesting" % "fest-swing-testng" % fest
  ).map(_ % "test")
}

libraryDependencies <++= scalaVersion{ v =>
  if(v.startsWith("2.10"))
    Seq(
      "org.scalatest" %% "scalatest" % "1.9.1" % "test"
     ,"org.scala-lang" % "scala-actors" % v
    )
  else
    Seq("org.scalatest" %% "scalatest" % "2.0.M5" % "test")
}

resolvers += "fest release" at "http://repository.codehaus.org"

scalacOptions ++= Seq(Opts.compile.deprecation, Opts.compile.unchecked)

publishMavenStyle := true

pomExtra :=
  <distributionManagement>
    <repository>
      <id>repo</id>
      <url>http://okomok.github.com/maven-repo/releases</url>
    </repository>
    <repository>
      <id>snapshot-repo</id>
      <url>http://okomok.github.com/maven-repo/snapshots</url>
    </repository>
  </distributionManagement>

