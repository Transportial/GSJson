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
version = "0.1.3"

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
    val signingKey = findProperty("signing.key").toString()
    val signingPassword = findProperty("signing.password").toString()
    useInMemoryPgpKeys(signingKey, signingPassword)
}

// Configure the vanniktech maven publish plugin
mavenPublishing {

//    configure(GradlePublishPlugin())

    coordinates(
        findProperty("GROUP").toString(),
        findProperty("POM_ARTIFACT_ID").toString(),
        version.toString()
    )

    pom {
        name.set(findProperty("POM_NAME").toString())
        description.set(findProperty("POM_DESCRIPTION").toString())
        url.set(findProperty("POM_URL").toString())
        inceptionYear.set("2024")

        licenses {
            license {
                name.set(findProperty("POM_LICENSE_NAME")?.toString() ?: Meta.license)
                url.set(findProperty("POM_LICENSE_URL").toString())
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }

        organization {
            name.set("Transportial BV")
            url.set("https://transportial.com")
        }

        developers {
            developer {
                id.set(findProperty("POM_DEVELOPER_ID").toString())
                name.set(findProperty("POM_DEVELOPER_NAME").toString())
                url.set(findProperty("POM_DEVELOPER_URL").toString())
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

// Add this task for automatic release
tasks.register("publishAndAutoRelease") {
    group = "publishing"
    description = "Publishes to Sonatype and automatically releases"
    dependsOn("publishAllPublicationsToMavenCentralRepository")
}