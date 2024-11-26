plugins {
  id("application")
  kotlin("jvm") version "2.0.21"

  id("io.gitlab.arturbosch.detekt") version "1.23.3"
}

group = "de.chrgroth"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  implementation(kotlin("reflect"))
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

  testImplementation(platform("org.junit:junit-bom:5.10.0"))
  testImplementation("org.junit.jupiter:junit-jupiter")

  testImplementation("org.assertj:assertj-core:3.23.1")
  testImplementation("io.mockk:mockk:1.13.9")
}

kotlin {
  jvmToolchain(17)
}

detekt {
  buildUponDefaultConfig = true
  config.setFrom(files("${rootProject.projectDir}/detekt-config.yaml"))
}

tasks.test {
  useJUnitPlatform()
  testLogging {
    events("passed", "skipped", "failed")
  }
}

application {
  mainClass = "de.chrgroth.adventofcode.MainKt"
}
