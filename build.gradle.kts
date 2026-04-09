plugins {
	java
	id("org.springframework.boot") version "4.0.3"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.rivercom"
version = "1.0.0"
description = "Backend service for Claw Machine rewards system"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
	maven {
		url = uri("https://jaspersoft.jfrog.io/jaspersoft/third-party-ce-artifacts/")
	}
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-h2console")
	implementation("net.sf.jasperreports:jasperreports:6.20.6")

	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	developmentOnly("org.springframework.boot:spring-boot-devtools")

	runtimeOnly("com.h2database:h2")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

val bootJarTask = tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar")
val currentOs = System.getProperty("os.name").lowercase()
val javaHome = System.getenv("JAVA_HOME") ?: System.getProperty("java.home")
val jlinkExecutable = file(
	if (currentOs.startsWith("windows")) "$javaHome/bin/jlink.exe" else "$javaHome/bin/jlink"
)

tasks.register<Copy>("prepareElectronJar") {
	dependsOn(bootJarTask)

	from(bootJarTask.flatMap { it.archiveFile })
	into(layout.buildDirectory.dir("electron/backend"))
	rename { "claw-machine-backend.jar" }
}

tasks.register<Exec>("prepareElectronRuntime") {
	onlyIf { jlinkExecutable.exists() }

	doFirst {
		val outputDir = layout.buildDirectory.dir("electron/runtime").get().asFile
		project.delete(outputDir)

		commandLine(
			jlinkExecutable.absolutePath,
			"--add-modules",
			"java.base,java.compiler,java.datatransfer,java.desktop,java.instrument,java.logging,java.management,java.naming,java.net.http,java.prefs,java.scripting,java.security.jgss,java.sql,java.xml,jdk.crypto.ec,jdk.unsupported",
			"--strip-debug",
			"--no-header-files",
			"--no-man-pages",
			"--compress=zip-6",
			"--output",
			outputDir.absolutePath
		)
	}
}

tasks.register("prepareElectronDist") {
	dependsOn("prepareElectronJar", "prepareElectronRuntime")
}
