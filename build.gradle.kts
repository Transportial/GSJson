import java.util.Base64

plugins {
    kotlin("jvm") version "1.9.23"
    `maven-publish`
    signing
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

object Meta {
    const val name = "gsjson"
    const val username = "transportial"
    const val desc = "GSJson is a getter/setter syntax interpretation language"
    const val license = "Apache-2.0"
    const val githubRepo = "transportial/gsjson"
}

group = "com.transportial"
version = "1.0.8"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("org.json:json:20240303")
    implementation("com.fasterxml.jackson.core:jackson-core:2.17.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}

// Add this task for automatic release
tasks.register("publishAndAutoRelease") {
    group = "publishing"
    description = "Publishes to Sonatype and automatically releases"

    dependsOn("publishToSonatype")

    doLast {
        val username = findProperty("centralPortalUsername") as String?
        val password = findProperty("centralPortalPassword") as String?

        if (username != null && password != null) {
            val credentials = Base64.getEncoder().encodeToString("$username:$password".toByteArray())

            exec {
                commandLine("curl", "-X", "POST",
                    "-H", "Authorization: Bearer $credentials",
                    "https://ossrh-staging-api.central.sonatype.com/manual/upload/defaultRepository/${project.group}?publishing_type=automatic"
                )
            }
            println("Automatic release initiated for ${project.group}:${project.name}:${project.version}")
        } else {
            println("Credentials not found. Please check your gradle.properties file.")
        }
    }
}

kotlin {
    jvmToolchain(17)
}

// Required for Maven Central - generate sources and javadoc JARs
java {
    withSourcesJar()
    withJavadocJar()
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set(Meta.name)
                description.set(Meta.desc)
                url.set("https://github.com/${Meta.githubRepo}")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                organization {
                    name.set("Transportial BV")
                    url.set("https://transportial.com")
                }

                developers {
                    developer {
                        id.set("thomaskolmans")
                        name.set("Thomas Kolmans")
                        url.set("https://github.com/thomaskolmans/")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/${Meta.githubRepo}.git")
                    developerConnection.set("scm:git:ssh://git@github.com/${Meta.githubRepo}.git")
                    url.set("https://github.com/${Meta.githubRepo}")
                }
            }
        }
    }
}

signing {
    val signingKey = findProperty("signing.key") as String?
    val signingKeyId = findProperty("signing.keyId") as String?
    val signingPassword = findProperty("signing.password") as String?
    val signingSecretKeyRingFile = findProperty("signing.secretKeyRingFile") as String?

    if (signingKey != null && signingPassword != null) {
        // Approach B: Using inline key content
        useInMemoryPgpKeys(signingKey, signingPassword)
    } else if (signingKeyId != null && signingPassword != null && signingSecretKeyRingFile != null) {
        // Approach A: Using key file
        useInMemoryPgpKeys(
            signingKeyId,
            File(signingSecretKeyRingFile).readText(),
            signingPassword
        )
    }
    sign(publishing.publications["maven"])
}

nexusPublishing {
    repositories {
        sonatype {
            // Correct URLs for Central Portal OSSRH Staging API
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
            username.set(findProperty("centralPortalUsername") as String? ?: System.getenv("CENTRAL_PORTAL_USERNAME"))
            password.set(findProperty("centralPortalPassword") as String? ?: System.getenv("CENTRAL_PORTAL_PASSWORD"))
        }
    }
}