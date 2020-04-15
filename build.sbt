organization := "pt.tecnico.dsi"
name := "scala-vault"
//version := "0.4.115-SNAPSHOT"

// ======================================================================================================================
// ==== Compile Options =================================================================================================
// ======================================================================================================================
javacOptions ++= Seq("-Xlint", "-encoding", "UTF-8", "-Dfile.encoding=utf-8")
scalaVersion := "2.13.1"

scalacOptions ++= Seq(
  "-encoding", "utf-8",            // Specify character encoding used by source files.
  "-explaintypes",                 // Explain type errors in more detail.
  "-feature",                      // Emit warning and location for usages of features that should be imported explicitly.
  "-language:implicitConversions", // Explicitly enables the implicit conversions feature
  "-Ybackend-parallelism", "8",    // Maximum worker threads for backend.
  "-Ybackend-worker-queue", "10",  // Backend threads worker queue size.
  "-Ymacro-annotations",           // Enable support for macro annotations, formerly in macro paradise.
  "-unchecked",                    // Enable additional warnings where generated code depends on assumptions.
  "-Xcheckinit",                   // Wrap field accessors to throw an exception on uninitialized access.
  // Unfortunately this is causing too much false positives
  //"-Xsource:2.14",                 // Treat compiler input as Scala source for the specified version.
  "-Xmigration:2.14",              // Warn about constructs whose behavior may have changed since version.
  "-Werror",                       // Fail the compilation if there are any warnings.
  "-Xlint:_",                      // Enables every warning. scalac -Xlint:help for a list and explanation
  "-Wunused:_",                    // Enables every warning of unused members/definitions/etc
  "-Wdead-code",                   // Warn when dead code is identified.
  "-Wextra-implicit",              // Warn when more than one implicit parameter section is defined.
  "-Wnumeric-widen",               // Warn when numerics are widened.
  "-Woctal-literal",               // Warn on obsolete octal syntax.
  //"-Wself-implicit",               // Warn when an implicit resolves to an enclosing self-definition.
  "-Wvalue-discard",               // Warn when non-Unit expression results are unused.
)
// These lines ensure that in sbt console or sbt test:console the -Ywarn* and the -Xfatal-warning are not bothersome.
// https://stackoverflow.com/questions/26940253/in-sbt-how-do-you-override-scalacoptions-for-console-in-all-configurations
scalacOptions in (Compile, console) ~= (_.filterNot { option =>
  option.startsWith("-W") || option.startsWith("-Xlint")
})
scalacOptions in (Test, console) := (scalacOptions in (Compile, console)).value

fork := true

// ======================================================================================================================
// ==== Dependencies ====================================================================================================
// ======================================================================================================================
libraryDependencies ++= Seq("blaze-client", "dsl", "circe").map { module =>
  "org.http4s"      %% s"http4s-$module" % "0.21.3"
} ++ Seq(
  "io.circe"        %% "circe-derivation" % "0.13.0-M4",
  "io.circe"        %% "circe-parser"     % "0.13.0", // Just used in WriteConcern
  "com.beachape"    %% "enumeratum-circe" % "1.5.23",
  "ch.qos.logback"  %  "logback-classic"  % "1.2.3" % Test,
  "org.scalatest"   %% "scalatest"        % "3.1.1" % Test,
)
addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")

Test / logBuffered := false
Test / fork := true
//coverageEnabled := true

// ======================================================================================================================
// ==== Scaladoc ========================================================================================================
// ======================================================================================================================
val latestReleasedVersion = SettingKey[String]("latest released version")
git.useGitDescribe := true
latestReleasedVersion := git.gitDescribedVersion.value.getOrElse("0.4.115-SNAPSHOT")

autoAPIMappings := true // Tell scaladoc to look for API documentation of managed dependencies in their metadata.
scalacOptions in (Compile, doc) ++= Seq(
  "-diagrams",    // Create inheritance diagrams for classes, traits and packages.
  "-groups",      // Group similar functions together (based on the @group annotation)
  "-implicits",   // Document members inherited by implicit conversions.
  "-doc-source-url", s"${homepage.value.get}/tree/v${latestReleasedVersion.value}€{FILE_PATH}.scala",
  "-sourcepath", (baseDirectory in ThisBuild).value.getAbsolutePath,
)
// Define the base URL for the Scaladocs for your library. This will enable clients of your library to automatically
// link against the API documentation using autoAPIMappings.
apiURL := Some(url(s"${homepage.value.get}/${latestReleasedVersion.value}/api/"))

enablePlugins(GhpagesPlugin)
siteSubdirName in SiteScaladoc := s"api/${version.value}"
envVars in ghpagesPushSite := Map("SBT_GHPAGES_COMMIT_MESSAGE" -> s"Add Scaladocs for version ${latestReleasedVersion.value}")
git.remoteRepo := s"git@github.com:ist-dsi/${name.value}.git"

// ======================================================================================================================
// ==== Deployment ======================================================================================================
// ======================================================================================================================
publishTo := sonatypePublishTo.value
sonatypeProfileName := organization.value

licenses += "MIT" -> url("http://opensource.org/licenses/MIT")
homepage := Some(url(s"https://github.com/ist-dsi/${name.value}"))
scmInfo := Some(ScmInfo(homepage.value.get, git.remoteRepo.value))
developers += Developer("Lasering", "Simão Martins", "", url("https://github.com/Lasering"))

// Will fail the build/release if updates for the dependencies are found
//dependencyUpdatesFailBuild := true

releaseUseGlobalVersion := false

releasePublishArtifactsAction := PgpKeys.publishSigned.value
import ReleaseTransformations._
releaseProcess := Seq[ReleaseStep](
  releaseStepTask(dependencyUpdates),
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  releaseStepTask(Compile / doc),
  releaseStepTask(Test / test), // For this to work "docker run --cap-add IPC_LOCK -d --name=dev-vault -p 8200:8200 vault"
  setReleaseVersion,
  tagRelease,
  releaseStepTask(ghpagesPushSite),
  publishArtifacts,
  releaseStepCommand("sonatypeRelease"),
  pushChanges,
  setNextVersion
)
