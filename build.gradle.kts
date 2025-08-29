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

//signing {
//
//    val signingKey = System.getenv("GPG_SIGNING_KEY")
//        ?.replace("\r\n", "\n")  // Normalize line endings
//        ?.replace("\\n", "\n")   // Handle escaped newlines
//
//    println("Has signing key: ${signingKey != null} and it contains ${signingKey?.length} characters")
//
//    val signingPassword = System.getenv("MAVEN_GPG_PASSPHRASE")
//
//    println("Has signing password: ${signingPassword != null} and it contains ${signingPassword?.length} characters")
//
//    if (signingKey != null && signingPassword != null) {
//        useInMemoryPgpKeys(signingKey, signingPassword)
//    }
//}

// Configure the vanniktech maven publish plugin
mavenPublishing {

    publishToMavenCentral(true)

    signAllPublications()


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
}

// Add this task for automatic release
tasks.register("publishAndAutoRelease") {
    group = "publishing"
    description = "Publishes to Sonatype and automatically releases"
    dependsOn("publishAllPublicationsToMavenCentralRepository")
}