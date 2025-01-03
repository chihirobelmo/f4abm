name := "ScalaLWJGL"

version := "0.1"

scalaVersion := "2.13.12"

libraryDependencies ++= Seq(
  "org.lwjgl" % "lwjgl" % "3.3.1",
  "org.lwjgl" % "lwjgl-opengl" % "3.3.1",
  "org.lwjgl" % "lwjgl-glfw" % "3.3.1",
  "org.lwjgl" % "lwjgl" % "3.3.1" classifier "natives-windows", // natives-macos | natives-linux
  "org.lwjgl" % "lwjgl-opengl" % "3.3.1" classifier "natives-windows",
  "org.lwjgl" % "lwjgl-glfw" % "3.3.1" classifier "natives-windows",
  "com.typesafe.akka" %% "akka-actor-typed" % "2.6.18",
  "com.typesafe.akka" %% "akka-stream" % "2.6.18"
)