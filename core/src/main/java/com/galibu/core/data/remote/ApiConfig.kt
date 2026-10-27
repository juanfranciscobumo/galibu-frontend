package com.galibu.core.data.remote

/**
 * Base URL of the Galibu backend API (must end with "/api/").
 * Default targets the Android emulator; physical devices override it at startup
 * from BuildConfig (local.properties `apiUrl`, e.g. your PC's LAN IP).
 */
object ApiConfig {
    var baseUrl: String = "http://10.0.2.2:8080/api/"
}
