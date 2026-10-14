package com.ashish.leadzam.data.api

/**
 * Configuration for the CRM Backend connection.
 */
object BackendConfig {

    /**
     * Base URL for the CRM backend server.
     * Note: "http://10.0.2.2:8080" is the standard Android emulator host IP pointing to localhost:8080.
     * On physical devices, replace with your local network IP (e.g., http://192.168.1.5:8080) or server domain.
     */
    var baseUrl: String = "http://10.0.2.2:8080"

    val registerDeviceEndpoint: String
        get() = "${baseUrl.trimEnd('/')}/api/register-device"
}
