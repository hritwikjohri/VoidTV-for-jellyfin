package com.hritwik.avoid.data.connection

import fi.iki.elonen.NanoHTTPD
import java.io.File
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class RemoteServerConfigPayload(
    @SerialName("connectionType") val connectionType: String? = null,
    @SerialName("fullUrl") val fullUrl: String? = null,
    @SerialName("mediaToken") val mediaToken: String? = null,
    @SerialName("quickConnectToken") val quickConnectToken: String? = null,
    @SerialName("quickConnectUrl") val quickConnectUrl: String? = null,
    @SerialName("port") val port: Int? = null,
    @SerialName("serverUrl") val serverUrl: String? = null
)

class RemoteConfigServer(
    listeningPort: Int,
    private val onPayloadReceived: (RemoteServerConfigPayload) -> Unit
) : NanoHTTPD(listeningPort) {

    override fun serve(session: IHTTPSession): Response {
        if (session.method != Method.POST) {
            return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, MIME_PLAINTEXT, "Method not allowed")
        }

        val files = mutableMapOf<String, String>()
        runCatching { session.parseBody(files) }

        val body = files["postData"]
        val filePayload = if (body.isNullOrBlank()) {
            files.entries
                .asSequence()
                .filter { (key, _) -> key != "postData" }
                .mapNotNull { (_, path) ->
                    runCatching { File(path).takeIf { it.exists() }?.readText() }
                        .getOrNull()
                        ?.takeIf { it.isNotBlank() }
                }
                .firstOrNull()
        } else null

        val payloadSource = body ?: filePayload

        if (payloadSource.isNullOrBlank()) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Empty payload")
        }

        val payload = runCatching {
            jsonDecoder.decodeFromString(RemoteServerConfigPayload.serializer(), payloadSource)
        }.getOrNull()

        if (payload == null) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Invalid payload")
        }

        onPayloadReceived(payload)

        return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\":\"received\"}")
    }

    companion object {
        private val jsonDecoder = Json { ignoreUnknownKeys = true }
    }
}