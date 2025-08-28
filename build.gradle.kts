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
version = "0.1.44"

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

    val signingKey = System.getenv("GPG_SIGNING_KEY")
        ?.replace("\r\n", "\n")  // Normalize line endings
        ?.replace("\\n", "\n")   // Handle escaped newlines

    val signingPassword = System.getenv("MAVEN_GPG_PASSPHRASE")

    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
    }
}

// Configure the vanniktech maven publish plugin
mavenPublishing {

//    configure(GradlePublishPlugin())

    coordinates(
        group.toString(),
        Meta.name,
        version.toString()
    )

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
                name.set("Transportial BV")
                url.set("https://transportial.com")
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
    val hasCredentials = (System.getenv("CENTRAL_TOKEN_USERNAME") != null && System.getenv("CENTRAL_TOKEN_PASSWORD") != null)

    println("Has credentials: $hasCredentials")
    publishToMavenCentral(automaticRelease = hasCredentials)
}

// Add this task for automatic release
tasks.register("publishAndAutoRelease") {
    group = "publishing"
    description = "Publishes to Sonatype and automatically releases"
    dependsOn("publishAllPublicationsToMavenCentralRepository")
}