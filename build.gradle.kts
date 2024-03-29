import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.Confidence
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

repositories {
    mavenCentral()
    // TODO: Remove when locc4j is published to Central
    maven("https://dist-4.lan/repository/public/")
}

plugins {
    `java-gradle-plugin`
    checkstyle
    jacoco
    `maven-publish`
    signing
    alias(libs.plugins.dependencyAnalysis)
    alias(libs.plugins.spotbugs)
    alias(libs.plugins.versions)
}

val baseVersion = "1.0.0"
val isSnapshot = true

val isCIServer = System.getenv("CTHING_CI") != null
val buildNumber = if (isCIServer) System.currentTimeMillis().toString() else "0"
version = if (isSnapshot) "$baseVersion-$buildNumber" else baseVersion
group = "org.cthing"
description = "A Gradle plugin for counting lines of code."

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
    }
}
gradlePlugin {
    website = "https://github.com/cthing/gradle-locc"
    vcsUrl = "https://github.com/cthing/gradle-locc"

    plugins {
        create("locc4jPlugin") {
            id = "org.cthing.locc"
            displayName = "Lines-Of-Code Counting Plugin"
            description = "Counts lines of code in a project. Uses the locc4j library which supports over 250 " +
                    "code languages and properly handles nested comments and embedded languages (e.g. CSS in HTML)."
            tags = listOf("counting", "lines of code", "loc", "locc4j", "metrics")
            implementationClass = "org.cthing.gradle.plugins.locc.LoccPlugin"
        }
    }
}

dependencies {
    api(libs.jsr305)
    api(libs.locc4j)

    implementation(libs.cthingAnnots)
    implementation(libs.jsonWriter)
    implementation(libs.xmlWriter)

    testImplementation(libs.assertJ)
    testImplementation(libs.commonsIO)
    testImplementation(libs.jacksonCoreUtils)
    testImplementation(libs.jacksonCore)
    testImplementation(libs.jacksonDatabind)
    testImplementation(libs.jacksonYaml)
    testImplementation(libs.jsonSchemaCore)
    testImplementation(libs.jsonSchemaValidator)
    testImplementation(libs.jsonUnit)
    testImplementation(libs.junitApi)
    testImplementation(libs.junitParams)
    testImplementation(libs.xmlAssertJ)
    testImplementation(libs.xmlPlaceholders)
    testImplementation(libs.xmlUnit)

    testRuntimeOnly(libs.junitEngine)
    testRuntimeOnly(libs.junitLauncher)

    spotbugsPlugins(libs.spotbugsContrib)
}

checkstyle {
    toolVersion = libs.versions.checkstyle.get()
    isIgnoreFailures = false
    configFile = file("dev/checkstyle/checkstyle.xml")
    configDirectory = file("dev/checkstyle")
    isShowViolations = true
}

spotbugs {
    toolVersion = libs.versions.spotbugs
    ignoreFailures = false
    effort = Effort.MAX
    reportLevel = Confidence.MEDIUM
    excludeFilter = file("dev/spotbugs/suppressions.xml")
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

dependencyAnalysis {
    issues {
        all {
            onAny {
                severity("fail")
            }
        }
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks {
    withType<JavaCompile> {
        options.release = libs.versions.java.get().toInt()
        options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-options", "-Werror"))
    }

    withType<Jar> {
        manifest.attributes(mapOf("Implementation-Title" to project.name,
                                  "Implementation-Vendor" to "C Thing Software",
                                  "Implementation-Version" to project.version))
    }

    withType<Javadoc> {
        val year = SimpleDateFormat("yyyy", Locale.ENGLISH).format(Date())
        with(options as StandardJavadocDocletOptions) {
            breakIterator(false)
            encoding("UTF-8")
            bottom("Copyright &copy; $year C Thing Software")
            addStringOption("Xdoclint:all,-missing", "-quiet")
            addStringOption("Werror", "-quiet")
            memberLevel = JavadocMemberLevel.PUBLIC
            outputLevel = JavadocOutputLevel.QUIET
        }
    }

    check {
        dependsOn(buildHealth)
    }

    spotbugsMain {
        reports.create("html").required = true
    }

    spotbugsTest {
        isEnabled = false
    }

    withType<JacocoReport> {
        dependsOn("test")
        with(reports) {
            xml.required = false
            csv.required = false
            html.required = true
            html.outputLocation = layout.buildDirectory.dir("reports/jacoco")
        }
    }

    withType<Test> {
        useJUnitPlatform()

        systemProperty("buildDir", layout.buildDirectory.get().asFile)
    }

    withType<GenerateModuleMetadata> {
        enabled = false
    }

    dependencyUpdates {
        revision = "release"
        gradleReleaseChannel = "current"
        outputFormatter = "plain,xml,html"
        outputDir = layout.buildDirectory.dir("reports/dependencyUpdates").get().asFile.absolutePath

        rejectVersionIf {
            isNonStable(candidate.version)
        }
    }
}

val sourceJar by tasks.registering(Jar::class) {
    from(project.sourceSets["main"].allSource)
    archiveClassifier = "sources"
}

val javadocJar by tasks.registering(Jar::class) {
    from(tasks.getByName("javadoc"))
    archiveClassifier = "javadoc"
}

publishing {
    publications {
        register("jar", MavenPublication::class) {
            from(components["java"])

            artifact(sourceJar)
            artifact(javadocJar)

            pom {
                name = project.name
                description = project.description
                url = "https://github.com/cthing/${project.name}"
                licenses {
                    license {
                        name = "Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0"
                    }
                }
                developers {
                    developer {
                        id = "baron"
                        name = "Baron Roberts"
                        email = "baron@cthing.com"
                        organization = "C Thing Software"
                        organizationUrl = "https://www.cthing.com"
                    }
                }
                scm {
                    connection = "scm:git:https://github.com/cthing/${project.name}.git"
                    developerConnection = "scm:git:git@github.com:cthing/${project.name}.git"
                    url = "https://github.com/cthing/${project.name}"
                }
                issueManagement {
                    system = "GitHub Issues"
                    url = "https://github.com/cthing/${project.name}/issues"
                }
            }
        }
    }

    val repoUrl = if (isSnapshot) findProperty("cthing.nexus.snapshotsUrl") else findProperty("cthing.nexus.candidatesUrl")
    if (repoUrl != null) {
        repositories {
            maven {
                name = "CThingMaven"
                setUrl(repoUrl)
                credentials {
                    username = property("cthing.nexus.user") as String
                    password = property("cthing.nexus.password") as String
                }
            }
        }
    }
}

if (hasProperty("signing.keyId") && hasProperty("signing.password") && hasProperty("signing.secretKeyRingFile")) {
    signing {
        sign(publishing.publications["jar"])
    }
}
