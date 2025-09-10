plugins {
    kotlin("jvm") version "1.9.20"
    `maven-publish`
    signing
    id("com.vanniktech.maven.publish") version "0.34.0"
    id("co.uzzu.dotenv.gradle") version "4.0.0"
}

object Meta {
    const val name = "GSJson"
    const val username = "transportial"
    const val desc = "GSJson is a getter/setter syntax interpretation language"
    const val license = "Apache-2.0"
    const val githubRepo = "transportial/gsjson"
}

group = "com.transportial"
version = "0.1.45"

repositories {
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
    jvmToolchain(8)
}

mavenPublishing {

    publishToMavenCentral(automaticRelease = true)
    signAllPublications()
}

mavenPublishing {
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
                distribution.set("repo")
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