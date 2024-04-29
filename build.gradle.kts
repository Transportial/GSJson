import com.vanniktech.maven.publish.SonatypeHost
import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar

plugins {
    kotlin("jvm") version "1.9.23"
    id("com.vanniktech.maven.publish") version "0.28.0"
}

object Meta {
    const val name = "gsjson"
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


mavenPublishing {
    configure(JavaLibrary(
        javadocJar = JavadocJar.Javadoc(),
        sourcesJar = true,
    ))

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    signAllPublications()

    coordinates(group.toString(), Meta.name, version.toString())

    pom {
        name.set(Meta.name)
        description.set(Meta.desc)
        inceptionYear.set("2024")
        url.set("https://github.com/${Meta.githubRepo}")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        organization {
            name = "Transportial BV"
            url = "https://transportial.com"
        }
        developers {
            developer {
                id.set("thomaskolmans")
                name.set("Thomas Kolmans")
                url.set("https://github.com/thomaskolmans/")
            }
        }
        scm {
            url.set("https://github.com/${Meta.githubRepo}")
            connection.set("scm:git:git://github.com/${Meta.githubRepo}.git")
            developerConnection.set("scm:git:ssh://git@github.com/${Meta.githubRepo}.git")
        }
    }
}
