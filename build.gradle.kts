import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestLogEvent.*

plugins {
  java
  application
  id("com.github.johnrengelman.shadow") version "7.1.0"
}

group = "com.fantasy0v0"
version = "1.0.0-SNAPSHOT"

repositories {
  mavenCentral()
}

val vertxVersion = "4.0.3"
val junitJupiterVersion = "5.7.0"

val mainVerticleName = "com.fantasy0v0.swagger_ui_server.MainVerticle"
val launcherClassName = "io.vertx.core.Launcher"

application {
  mainClassName = launcherClassName
}

dependencies {
  implementation(platform("io.vertx:vertx-stack-depchain:$vertxVersion"))
  implementation("io.vertx:vertx-core")
  implementation("io.vertx:vertx-web")
  testImplementation("io.vertx:vertx-junit5")
  testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
}

java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<ShadowJar> {
  archiveClassifier.set("fat")
  manifest {
    attributes(mapOf("Main-Verticle" to mainVerticleName))
  }
  mergeServiceFiles()
}

tasks.withType<Test> {
  useJUnitPlatform()
  testLogging {
    events = setOf(PASSED, SKIPPED, FAILED)
  }
}

tasks.withType<JavaExec> {
  args = listOf("run", mainVerticleName)
}

tasks.withType<JavaCompile> {
  options.encoding = "UTF-8"
}

task<Delete>("deleteWebRoot") {
  delete("./src/main/resources/webroot")
}

task<Exec>("buildUi") {
  workingDir("./ui")
  commandLine("cmd", "/c", "pnpm", "run", "build")

  dependsOn("deleteWebRoot")
}

task<Copy>("copyUi") {
  from("./ui/build")
  into("./src/main/resources/webroot")

  dependsOn("buildUi")
}

tasks.withType<ProcessResources> {
  dependsOn("copyUi")
}
