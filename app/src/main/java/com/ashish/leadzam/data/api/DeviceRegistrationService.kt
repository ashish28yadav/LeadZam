package com.ashish.leadzam.data.api

import android.content.Context
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

enum class BackendRegistrationStatus {
    NOT_REGISTERED,
    REGISTERING,
    REGISTERED,
    FAILED
}

data class RegistrationResult(
    val isSuccess: Boolean,
    val statusCode: Int = -1,
    val message: String
)

object DeviceRegistrationService {

    private const val TAG = "DeviceRegService"

    /**
     * Posts the FCM token and device ID to the backend endpoint POST /api/register-device.
     */
    suspend fun registerDevice(
        context: Context,
        fcmToken: String
    ): RegistrationResult = withContext(Dispatchers.IO) {
        if (fcmToken.isBlank()) {
            return@withContext RegistrationResult(
                isSuccess = false,
                message = "FCM token is empty."
            )
        }

        val endpointUrl = BackendConfig.registerDeviceEndpoint
        val deviceId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
        } catch (e: Exception) {
            "unknown_device"
        }

        try {
            Log.d(TAG, "Attempting device registration at endpoint: $endpointUrl")
            val url = URL(endpointUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.doOutput = true

            val payload = JSONObject().apply {
                put("token", fcmToken)
                put("deviceId", deviceId)
                put("platform", "android")
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            val isSuccess = responseCode in 200..299

            val responseBody = if (isSuccess) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            }

            Log.d(TAG, "Backend HTTP $responseCode Response: $responseBody")

            if (isSuccess) {
                RegistrationResult(
                    isSuccess = true,
                    statusCode = responseCode,
                    message = "Registered successfully with backend."
                )
            } else {
                RegistrationResult(
                    isSuccess = false,
                    statusCode = responseCode,
                    message = "Backend returned HTTP $responseCode: ${responseBody.ifBlank { "Registration failed" }}"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to backend at $endpointUrl", e)
            RegistrationResult(
                isSuccess = false,
                message = "Connection failed: ${e.localizedMessage ?: e.message}. Ensure backend server is running at $endpointUrl."
            )
        }
    }
}
