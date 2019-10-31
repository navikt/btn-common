val btnBomVersion = "3"

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.3.50"
    `java-library`
    `maven-publish`
}

val githubUser: String by project
val githubPassword: String by project

repositories {
    jcenter()
    mavenCentral()
    maven("https://dl.bintray.com/kotlin/ktor")
    maven("http://packages.confluent.io/maven/")
    maven {
        url = uri("https://maven.pkg.github.com/navikt/btn-common")
        credentials {
            username = githubUser
            password = githubPassword
        }
    }
}

version = properties["version"] ?: "local"

dependencies {
    implementation(kotlin("stdlib"))
    implementation(platform("no.nav.btn:btn-bom:$btnBomVersion"))
    implementation("com.google.code.gson:gson")
    implementation("org.apache.kafka:kafka-clients")
    implementation("io.prometheus:simpleclient_hotspot")
    implementation("io.prometheus:simpleclient_common")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.ktor:ktor-server-netty")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

publishing {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/navikt/btn-common")
            credentials {
                username = System.getenv("GITHUB_USERNAME")
                password = System.getenv("GITHUB_PASSWORD")
            }
        }
    }
    publications {
        create<MavenPublication>("mavenJava") {

            pom {
                name.set("btn-common")
                description.set("Bibliotek for kafka konsumenter og produsenter på BTN (Beskjed Til NAV)")
                url.set("https://github.com/navikt/btn-common")
                groupId = "no.nav.btn"
                artifactId = "btn-common"
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                scm {
                    connection.set("scm:git:https://github.com/navikt/btn-common.git")
                    developerConnection.set("scm:git:https://github.com/navikt/btn-common.git")
                    url.set("https://github.com/navikt/btn-common")
                }
            }
            from(components["java"])
        }
    }
}