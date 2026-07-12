plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.maven.publish)
}

val liburingDir = rootProject.layout.projectDirectory.dir("external/liburing")
val liburingInstallDir = layout.projectDirectory.dir("build/liburing-install")

val buildLiburing by tasks.registering(Exec::class) {
    workingDir = liburingDir.asFile

    commandLine(
        "bash",
        "-lc",
        """
        ./configure --prefix=${liburingInstallDir.asFile.absolutePath}
        make -j$(nproc)

        mkdir -p ${liburingInstallDir.asFile.absolutePath}/include
        mkdir -p ${liburingInstallDir.asFile.absolutePath}/lib

        cp -r src/include/* ${liburingInstallDir.asFile.absolutePath}/include/
        cp src/liburing.a ${liburingInstallDir.asFile.absolutePath}/lib/
        """.trimIndent()
    )

    outputs.dir(liburingInstallDir)
}
val generatedUringDef = layout.buildDirectory.file("generated/cinterop/uring.def")
val generateUringDef by tasks.registering {
    dependsOn(buildLiburing)
    outputs.file(generatedUringDef)

    doLast {
        generatedUringDef.get().asFile.parentFile.mkdirs()
        generatedUringDef.get().asFile.writeText(
            """
            headers = liburing.h
            package = linux.uring

            compilerOpts = -I${liburingInstallDir.dir("include").asFile.absolutePath} -I/usr/include -I/usr/include/x86_64-linux-gnu
            staticLibraries = liburing.a
            libraryPaths = ${liburingInstallDir.dir("lib").asFile.absolutePath}
            """.trimIndent()
        )
    }
}

kotlin {
    listOf(
        linuxX64(),
        linuxArm64()
    ).forEach {
        it.compilations.getByName("main") {
            cinterops {
                val uring by creating {
                    defFile(generateUringDef.map {
                        generatedUringDef.get().asFile
                    })
                }
            }
        }
    }
}

tasks.matching { it.name.startsWith("cinteropUring") }.configureEach {
    dependsOn(generateUringDef)
}

mavenPublishing {
    signAllPublications()
    publishToMavenCentral()

    pom {
        name.set("io-uring-kotlin")
        description.set("Kotlin Native klib for io-uring")
        url.set("https://github.com/andannn/RaylibKt")

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
            url.set("https://github.com/kio-labs/io-uring-kotlin.git")
            connection.set("scm:git:git://github.com/kio-labs/io-uring-kotlin.git")
            developerConnection.set("scm:git:ssh://git@github.com/kio-labs/io-uring-kotlin.git")
        }
    }
}