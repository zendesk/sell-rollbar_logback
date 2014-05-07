import SonatypeKeys._

// Import default settings. This changes `publishTo` settings to use the Sonatype repository and add several commands for publishing.
sonatypeSettings

name := "rollbar-logback"

version := "1.0"

libraryDependencies ++= Seq(
  "org.slf4j"       % "slf4j-api"       % "1.7.7",
  "ch.qos.logback"  % "logback-classic" % "1.1.2",
  "ch.qos.logback"  % "logback-core"    % "1.1.2",
  "javax.servlet"   % "servlet-api"     % "2.5",
  "org.json"        % "json"            % "20140107"
)

organization :=  "com.github.ahaid"

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := (<url>https://github.com/ahaid/rollbar-logback</url>
  <licenses>
    <license>
      <name>Apache 2</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:ahaid/rollbar-logback.git</url>
    <connection>scm:git:git@github.com:ahaid/rollbar-logback.git</connection>
  </scm>
  <developers>
    <developer>
      <id>ahaid</id>
      <name>Adam Haid</name>
      <url>https://github.com/ahaid</url>
    </developer>
  </developers>)
