plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    macosArm64() {
        binaries {
            executable {
                entryPoint("main")
            }
        }
    }

    sourceSets {
        nativeMain.dependencies {
            implementation(project(":openssl"))
        }
    }
}
