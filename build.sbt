organization := "pt.tecnico.dsi"
name := "scala-vault"

// ======================================================================================================================
// ==== Compile Options =================================================================================================
// ======================================================================================================================
javacOptions ++= Seq("-Xlint", "-encoding", "UTF-8", "-Dfile.encoding=utf-8")
scalaVersion := "2.13.10"

scalacOptions ++= Seq(
  "-encoding", "utf-8",            // Specify character encoding used by source files.
  "-explaintypes",                 // Explain type errors in more detail.
  "-feature",                      // Emit warning and location for usages of features that should be imported explicitly.
  "-Ybackend-parallelism", "8",    // Maximum worker threads for backend.
  "-Ybackend-worker-queue", "10",  // Backend threads worker queue size.
  "-Ymacro-annotations",           // Enable support for macro annotations, formerly in macro paradise.
  "-unchecked",                    // Enable additional warnings where generated code depends on assumptions.
  "-Xcheckinit",                   // Wrap field accessors to throw an exception on uninitialized access.
  "-Xsource:3",                    // Treat compiler input as Scala source for the specified version.
  "-Xmigration:3",                 // Warn about constructs whose behavior may have changed since version.
  "-Werror",                       // Fail the compilation if there are any warnings.
  "-Xlint:_",                      // Enables every warning. scalac -Xlint:help for a list and explanation
  "-Wunused:_",                    // Enables every warning of unused members/definitions/etc
  "-Wdead-code",                   // Warn when dead code is identified.
  "-Wextra-implicit",              // Warn when more than one implicit parameter section is defined.
  "-Wnumeric-widen",               // Warn when numerics are widened.
  "-Wvalue-discard",               // Warn when non-Unit expression results are unused.
)
// These lines ensure that in sbt console or sbt test:console the -Ywarn* and the -Xfatal-warning are not bothersome.
// https://stackoverflow.com/questions/26940253/in-sbt-how-do-you-override-scalacoptions-for-console-in-all-configurations
Compile / console / scalacOptions ~= (_.filterNot { option =>
  option.startsWith("-W") || option.startsWith("-Xlint")
})
Test / console / scalacOptions := (Compile / console / scalacOptions).value

fork := true

// ======================================================================================================================
// ==== Dependencies ====================================================================================================
// ======================================================================================================================
libraryDependencies ++= Seq("blaze-client", "circe").map { module =>
  "org.http4s"      %% s"http4s-$module" % "1.0.0-M21"
} ++ Seq(
  "io.circe"        %% "circe-derivation" % "0.13.0-M5",
  "io.circe"        %% "circe-parser"     % "0.13.0", // Just used in Databases
  "com.beachape"    %% "enumeratum-circe" % "1.6.1",
  "ch.qos.logback"  %  "logback-classic"  % "1.4.7" % Test,
  "org.scalatest"   %% "scalatest"        % "3.2.16" % Test,
)
addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")

// http://www.scalatest.org/user_guide/using_the_runner
//   -o[configs...] - causes test results to be written to the standard output.
//      D - show all durations
//      F - show full stack traces
Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-oDF")

Test / logBuffered := false
Test / fork := true
//coverageEnabled := true

// ======================================================================================================================
// ==== Scaladoc ========================================================================================================
// ======================================================================================================================
git.remoteRepo := s"git@github.com:ist-dsi/${name.value}.git"
val latestReleasedVersion = SettingKey[String]("latest released version")
latestReleasedVersion := git.gitDescribedVersion.value.getOrElse("0.0.1-SNAPSHOT")

// Define the base URL for the Scaladocs for your library. This will enable clients of your library to automatically
// link against the API documentation using autoAPIMappings.
apiURL := Some(url(s"${homepage.value.get}/api/${latestReleasedVersion.value}/"))
autoAPIMappings := true // Tell scaladoc to look for API documentation of managed dependencies in their metadata.
Compile / doc / scalacOptions ++= Seq(
  "-author",      // Include authors.
  "-diagrams",    // Create inheritance diagrams for classes, traits and packages.
  "-groups",      // Group similar functions together (based on the @group annotation)
  "-implicits",   // Document members inherited by implicit conversions.
  "-doc-title", name.value.capitalize,
  "-doc-version", latestReleasedVersion.value,
  "-doc-source-url", s"${homepage.value.get}/tree/v${latestReleasedVersion.value}€{FILE_PATH}.scala",
  "-sourcepath", baseDirectory.value.getAbsolutePath,
)

enablePlugins(GhpagesPlugin, SiteScaladocPlugin)
SiteScaladoc / siteSubdirName := s"api/${version.value}"
ghpagesCleanSite / excludeFilter := AllPassFilter // We want to keep all the previous API versions
val latestFileName = "latest"
val createLatestSymlink = taskKey[Unit](s"Creates a symlink named $latestFileName which points to the latest version.")
createLatestSymlink := {
  import java.nio.file.Files
  // We use ghpagesSynchLocal instead of ghpagesRepository to ensure the files in the local filesystem already exist
  val linkName = (ghpagesSynchLocal.value / "api" / latestFileName).toPath
  val target = new File(latestReleasedVersion.value).toPath
  if (!(Files.isSymbolicLink(linkName) && Files.readSymbolicLink(linkName) == target)) {
    Files.delete(linkName)
    Files.createSymbolicLink(linkName, target)
  }
}
ghpagesPushSite := ghpagesPushSite.dependsOn(createLatestSymlink).value
ghpagesBranch := "gh-pages"
ghpagesNoJekyll := false
ghpagesPushSite / envVars := Map("SBT_GHPAGES_COMMIT_MESSAGE" -> s"Add Scaladocs for version ${latestReleasedVersion.value}")

// ======================================================================================================================
// ==== Deployment ======================================================================================================
// ======================================================================================================================
credentials += Credentials(Path.userHome / ".sbt" / "sonatype_credentials")

publishTo := {
  // For accounts created after Feb 2021:
  // val nexus = "https://s01.oss.sonatype.org/"
  val nexus = "https://fenix-ashes.tecnico.ulisboa.pt/nexus/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "repository/dsi-private-repo")
}


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
  //releaseStepTask(dependencyUpdates),
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
