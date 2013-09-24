import com.typesafe.sbt.SbtStartScript

seq(SbtStartScript.startScriptForClassesSettings: _*)

organization := "com.kenbritton"

name := "ken.ly"

version := "1.0"

scalaVersion := "2.10.2"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

parallelExecution in Test := false

resolvers ++= Seq(
  "spray repo" at "http://repo.spray.io/"
)

libraryDependencies ++= Seq(
  "io.spray"            %   "spray-can"     % "1.2-M8",
  "io.spray"            %   "spray-routing" % "1.2-M8",
  "io.spray"            %   "spray-testkit" % "1.2-M8" % "test",
  "io.spray" 			%%  "spray-json" 	% "1.2.5",
  "com.typesafe.akka"   %%  "akka-actor"    % "2.2.1",
  "com.typesafe.akka"   %%  "akka-testkit"  % "2.2.1" % "test",
  "org.mongodb" 		%% 	"casbah" 		% "2.6.3",
  "org.slf4j" 			% 	"slf4j-simple" 	% "1.6.2",
  "org.specs2"          %%  "specs2"        % "1.14" % "test"
)

seq(Revolver.settings: _*)