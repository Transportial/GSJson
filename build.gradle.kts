import com.vanniktech.maven.publish.GradlePublishPlugin
import java.util.Base64

plugins {
    kotlin("jvm") version "1.9.23"
    signing
    id("com.vanniktech.maven.publish") version "0.34.0"
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

kotlin {
    jvmToolchain(17)
}

signing {
    // These property names must match the environment variables from the GitHub workflow
    val signingKey = findProperty("signingInMemoryKey")?.toString()
    val signingPassword = findProperty("signingInMemoryKeyPassword")?.toString()
    useInMemoryPgpKeys(signingKey, signingPassword)
}

// Configure the vanniktech maven publish plugin
mavenPublishing {

    // Use project properties and the Meta object directly for better clarity and robustness
    coordinates(project.group.toString(), Meta.name, project.version.toString())

    pom {
        name.set(Meta.name)
        description.set(Meta.desc)
        url.set("https://github.com/${Meta.githubRepo}")
        inceptionYear.set("2024")

        licenses {
            license {
                name.set(Meta.license)
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
                id.set(Meta.username)
                name.set("T. de Boer") // Or your name
                url.set("https://github.com/${Meta.username}")
            }
        }

        scm {
            connection.set("scm:git:git://github.com/${Meta.githubRepo}.git")
            developerConnection.set("scm:git:ssh://git@github.com/${Meta.githubRepo}.git")
            url.set("https://github.com/${Meta.githubRepo}")
        }
    }

    // Configure signing
    signAllPublications()

    // Only configure Maven Central publishing if credentials are available
    val hasCredentials = findProperty("mavenCentralUsername") != null &&
            findProperty("mavenCentralPassword") != null

    if (hasCredentials) {
        publishToMavenCentral(automaticRelease = true)
    }
}