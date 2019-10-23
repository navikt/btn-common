plugins {
    id("org.jetbrains.kotlin.jvm") version "1.3.50"
    `java-library`
    `maven-publish`
}

repositories {
    jcenter()
}

version = properties["version"] ?: "local"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

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
                description.set("Bibliotek for tidslinjer av intervaller relatert til sykefravær")
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