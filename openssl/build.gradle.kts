plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.maven.publish)
}

val liburingDir = rootProject.layout.projectDirectory.dir("external/openssl")
val libsslInstallDir = layout.projectDirectory.dir("build/openssl-install")

val buildOpenssl by tasks.registering(Exec::class) {
    workingDir = liburingDir.asFile

    commandLine(
        "bash",
        "-lc",
        """
        ./configure --prefix=${libsslInstallDir.asFile.absolutePath}
        make -j$(nproc)

        mkdir -p ${libsslInstallDir.asFile.absolutePath}/include
        mkdir -p ${libsslInstallDir.asFile.absolutePath}/lib

        make install_sw
        """.trimIndent()
    )

    outputs.dir(libsslInstallDir)
}
val generatedOpensslDef = layout.buildDirectory.file("generated/cinterop/openssl.def")
val macosArm64LibPath = layout.projectDirectory.dir("lib/macosArm64")
val linuxX64LibPath = layout.projectDirectory.dir("lib/linuxX64")
val generateOpensslDef by tasks.registering {
    outputs.file(generatedOpensslDef)

    doLast {
        generatedOpensslDef.get().asFile.parentFile.mkdirs()
        generatedOpensslDef.get().asFile.writeText(
            """
            headers = openssl/ssl.h openssl/err.h
            package = openssl
            staticLibraries = libssl.a libcrypto.a

            compilerOpts.macos_arm64 = -I${macosArm64LibPath.dir("include").asFile.absolutePath} 
            libraryPaths.macos_arm64 = ${macosArm64LibPath.dir("lib").asFile.absolutePath}
            """.trimIndent()
        )
    }
}

kotlin {
    listOf(
        linuxX64(),
        macosArm64()
    ).forEach {
        it.compilations.getByName("main") {
            cinterops {
                val openssl by creating {
                    defFile(generateOpensslDef.map {
                        generatedOpensslDef.get().asFile
                    })
                }
            }
        }
    }
}

tasks.matching { it.name.startsWith("cinteropOpenssl") }.configureEach {
    dependsOn(generateOpensslDef)
}

mavenPublishing {
    signAllPublications()
    publishToMavenCentral()

    pom {
        name.set("openssl-kotlin")
        description.set("Kotlin Native klib for openssl")
        url.set("https://github.com/openssl/openssl#build-and-install")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }

        developers {
            developer {
                id.set("andannn")
                name.set("Andannn")
            }
        }

        scm {
            url.set("https://github.com/kio-labs/openssl-kotlin.git")
            developerConnection.set("scm:git:ssh://git@github.com/kio-labs/openssl-kotlin.git")
            connection.set("scm:git:git://github.com/kio-labs/openssl-kotlin.git")
        }
    }
}