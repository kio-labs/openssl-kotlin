@file:OptIn(ExperimentalForeignApi::class)

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import openssl.OPENSSL_VERSION
import openssl.OpenSSL_version

fun main() {
    val version = OpenSSL_version(OPENSSL_VERSION)?.toKString()
    println("version: $version")
}