import sbt.Keys._

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.5.2")

name := "rollbar-logback"

version := "0.1-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.slf4j"       % "slf4j-api"       % "1.7.7",
  "ch.qos.logback"  % "logback-classic" % "1.1.2",
  "ch.qos.logback"  % "logback-core"    % "1.1.2",
  "javax.servlet"   % "servlet-api"     % "2.5",
  "org.json"        % "json"            % "20140107"
)