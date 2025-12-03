package com.hritwik.avoid.data.connection

import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.util.Base64
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class RemoteServerConfigPayload(
    @SerialName("connectionType") val connectionType: String? = null,
    @SerialName("fullUrl") val fullUrl: String? = null,
    @SerialName("mediaToken") val mediaToken: String? = null,
    @SerialName("password") val password: String? = null,
    @SerialName("quickConnectToken") val quickConnectToken: String? = null,
    @SerialName("quickConnectUrl") val quickConnectUrl: String? = null,
    @SerialName("port") val port: Int? = null,
    @SerialName("serverUrl") val serverUrl: String? = null,
    @SerialName("mtlsEnabled") val mtlsEnabled: Boolean? = null,
    @SerialName("mtlsCertificate") val mtlsCertificate: String? = null,
    @SerialName("mtlsCertificateBase64") val mtlsCertificateBase64: String? = null,
    @SerialName("mtlsCertificateName") val mtlsCertificateName: String? = null,
    @SerialName("mtlsCertificatePassword") val mtlsCertificatePassword: String? = null,
    @SerialName("username") val username: String? = null
)

class RemoteConfigServer(
    listeningPort: Int,
    private val onPayloadReceived: (RemoteServerConfigPayload) -> Unit
) : NanoHTTPD(listeningPort) {

    override fun serve(session: IHTTPSession): Response {
        if (session.method == Method.GET || session.method == Method.HEAD) {
            return newFixedLengthResponse(Response.Status.OK, MIME_HTML, CONFIG_PAGE_HTML)
        }

        if (session.method == Method.OPTIONS) {
            return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "")
                .also { it.addHeader("Allow", "GET,HEAD,POST,OPTIONS") }
        }

        if (session.method != Method.POST) {
            return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, MIME_PLAINTEXT, "Method not allowed")
        }

        val contentLength = session.headers["content-length"]?.toLongOrNull()
        if (contentLength != null && contentLength > MAX_UPLOAD_BYTES) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Payload too large")
        }

        val files = mutableMapOf<String, String>()
        val parseResult = runCatching { session.parseBody(files) }
        if (parseResult.isFailure) {
            val reason = parseResult.exceptionOrNull()?.message ?: "Unable to read request body"
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, reason)
        }

        val payloadResult = parsePayload(session, files)
        val payload = payloadResult.getOrElse { error ->
            val reason = error.message ?: "Invalid payload"
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, reason)
        }

        onPayloadReceived(payload)

        return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\":\"received\"}")
    }

    companion object {
        private val jsonDecoder = Json { ignoreUnknownKeys = true }
        private const val MAX_UPLOAD_BYTES = 1_500_000L
        private const val MAX_CERT_BYTES = 512 * 1024
        private const val MIME_HTML = "text/html; charset=utf-8"

        private val CONFIG_PAGE_HTML = """
            <!doctype html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>VoidTV Remote Setup</title>
              <style>
                :root {
                    --bg-dark: #0f172a;
                    --bg-card: #1e293b;
                    --input-bg: #0b1121;
                    --border: #334155;
                    --border-focus: #38bdf8;
                    --text-main: #f1f5f9;
                    --text-muted: #94a3b8;
                    --primary: #0ea5e9;
                    --primary-hover: #0284c7;
                    --success-bg: #064e3b;
                    --success-text: #6ee7b7;
                    --error-bg: #7f1d1d;
                    --error-text: #fca5a5;
                }
                
                * { box-sizing: border-box; }
                
                body { 
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                    background: radial-gradient(circle at top center, #1e293b 0%, #0f172a 100%);
                    color: var(--text-main);
                    margin: 0;
                    padding: 20px;
                    min-height: 100vh;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                }

                .card {
                    width: 100%;
                    max-width: 600px;
                    background: rgba(30, 41, 59, 0.7);
                    backdrop-filter: blur(12px);
                    -webkit-backdrop-filter: blur(12px);
                    border: 1px solid rgba(255, 255, 255, 0.1);
                    border-radius: 16px;
                    padding: 32px;
                    box-shadow: 0 20px 40px rgba(0,0,0,0.4);
                }

                header {
                    margin-bottom: 24px;
                    text-align: center;
                }

                h1 { 
                    margin: 0 0 8px 0; 
                    font-size: 24px; 
                    font-weight: 700; 
                    background: linear-gradient(to right, #fff, #94a3b8);
                    -webkit-background-clip: text;
                    -webkit-text-fill-color: transparent;
                }
                
                p.subtitle {
                    margin: 0;
                    color: var(--text-muted);
                    font-size: 14px;
                }

                .section-title {
                    font-size: 12px;
                    text-transform: uppercase;
                    letter-spacing: 1px;
                    color: var(--text-muted);
                    margin: 24px 0 12px 0;
                    font-weight: 600;
                    border-bottom: 1px solid var(--border);
                    padding-bottom: 8px;
                }

                .grid { 
                    display: grid; 
                    grid-template-columns: 1fr; 
                    gap: 16px; 
                }
                
                @media(min-width: 600px) {
                    .grid.two-col { grid-template-columns: 1fr 1fr; }
                }

                .form-group {
                    display: flex;
                    flex-direction: column;
                    gap: 6px;
                }

                label { 
                    font-size: 13px; 
                    color: var(--text-main); 
                    font-weight: 500;
                }

                input, select { 
                    width: 100%; 
                    padding: 12px 14px; 
                    border-radius: 8px; 
                    border: 1px solid var(--border); 
                    background: var(--input-bg); 
                    color: white; 
                    font-size: 14px;
                    transition: all 0.2s ease;
                }

                input:focus, select:focus {
                    outline: none;
                    border-color: var(--border-focus);
                    box-shadow: 0 0 0 2px rgba(56, 189, 248, 0.2);
                }
                
                input::placeholder {
                    color: #475569;
                }

                input[type="file"] {
                    padding: 8px;
                    background: var(--bg-card);
                }
                
                input[type="file"]::file-selector-button {
                    background: var(--border);
                    border: none;
                    color: var(--text-main);
                    padding: 4px 12px;
                    border-radius: 4px;
                    margin-right: 12px;
                    cursor: pointer;
                    transition: background 0.2s;
                }
                
                input[type="file"]::file-selector-button:hover {
                    background: #475569;
                }

                button { 
                    margin-top: 32px; 
                    width: 100%; 
                    padding: 14px; 
                    border: none; 
                    border-radius: 10px; 
                    background: linear-gradient(135deg, var(--primary), var(--primary-hover)); 
                    color: white; 
                    font-size: 15px; 
                    font-weight: 600; 
                    cursor: pointer; 
                    transition: transform 0.1s, opacity 0.2s;
                    box-shadow: 0 4px 12px rgba(14, 165, 233, 0.3);
                }

                button:hover { 
                    opacity: 0.95; 
                }
                
                button:active {
                    transform: scale(0.98);
                }

                button:disabled { 
                    opacity: 0.6; 
                    cursor: not-allowed; 
                    transform: none;
                }

                .status { 
                    margin-top: 16px; 
                    padding: 12px; 
                    border-radius: 8px; 
                    font-size: 14px; 
                    display: none; 
                    text-align: center;
                    animation: fadeIn 0.3s ease;
                }

                .status.ok { background: rgba(6, 78, 59, 0.5); color: var(--success-text); border: 1px solid var(--success-bg); }
                .status.err { background: rgba(127, 29, 29, 0.5); color: var(--error-text); border: 1px solid var(--error-bg); }

                small { 
                    display: block; 
                    margin-top: 4px; 
                    color: var(--text-muted); 
                    font-size: 12px; 
                }
                
                @keyframes fadeIn {
                    from { opacity: 0; transform: translateY(-5px); }
                    to { opacity: 1; transform: translateY(0); }
                }
              </style>
            </head>
            <body>
              <div class="card">
                <header>
                    <h1>Configure VoidTV</h1>
                    <p class="subtitle">Enter connection details below.</p>
                </header>
                
                <form id="config-form">
                  
                  <div class="section-title">Connection</div>
                  <div class="grid">
                    <div class="form-group">
                      <label for="serverUrl">Server URL / Host</label>
                      <input id="serverUrl" name="serverUrl" placeholder="e.g. 192.168.1.100 or example.com" />
                    </div>
                  </div>
                  <div class="grid two-col" style="margin-top: 16px;">
                    <div class="form-group">
                      <label for="port">Port <span style="font-weight:normal; color:var(--text-muted);">(Optional)</span></label>
                      <input id="port" name="port" type="number" min="1" max="65535" placeholder="8096" />
                    </div>
                    <div class="form-group">
                      <label for="connectionType">Type</label>
                      <select id="connectionType" name="connectionType">
                        <option value="">Auto</option>
                        <option value="http">HTTP</option>
                        <option value="https">HTTPS</option>
                      </select>
                    </div>
                  </div>

                  <div class="section-title">Authentication (Optional)</div>
                  <div class="grid two-col">
                    <div class="form-group">
                      <label for="username">Username</label>
                      <input id="username" name="username" autocomplete="username" placeholder="User" />
                    </div>
                    <div class="form-group">
                      <label for="password">Password</label>
                      <input id="password" name="password" type="password" autocomplete="current-password" placeholder="••••••" />
                    </div>
                  </div>

                  <div class="section-title">mTLS Security (Optional)</div>
                  <div class="grid">
                    <div class="form-group">
                      <label for="mtlsCertificate">Certificate File</label>
                      <input id="mtlsCertificate" name="mtlsCertificate" type="file" accept=".p12,.pfx,.pem,.crt,.cer" />
                      <small>Supported: .p12, .pfx</small>
                    </div>
                    <div class="form-group">
                      <label for="mtlsCertificatePassword">Certificate Password</label>
                      <input id="mtlsCertificatePassword" name="mtlsCertificatePassword" type="password" placeholder="Cert password if encrypted" />
                    </div>
                  </div>

                  <button type="submit" id="submit-btn">Send Configuration</button>
                  <div id="status" class="status"></div>
                </form>
              </div>

              <script>
                const form = document.getElementById('config-form');
                const statusBox = document.getElementById('status');
                const button = document.getElementById('submit-btn');

                function setStatus(message, ok) {
                  statusBox.style.display = 'block';
                  statusBox.textContent = message;
                  statusBox.className = 'status ' + (ok ? 'ok' : 'err');
                }

                form.addEventListener('submit', async (event) => {
                  event.preventDefault();
                  statusBox.style.display = 'none';
                  button.disabled = true;
                  button.innerHTML = 'Sending...';
                  
                  const data = new FormData(form);
                  try {
                    const res = await fetch('/', { method: 'POST', body: data });
                    const text = await res.text();
                    if (res.ok) {
                      setStatus('Configuration sent successfully!', true);
                      button.innerHTML = 'Sent';
                    } else {
                      setStatus('Failed: ' + text, false);
                      button.innerHTML = 'Try Again';
                    }
                  } catch (err) {
                    setStatus('Request error: ' + err, false);
                    button.innerHTML = 'Try Again';
                  } finally {
                    button.disabled = false;
                  }
                });
              </script>
            </body>
            </html>
        """.trimIndent()
    }

    private fun parsePayload(
        session: IHTTPSession,
        files: Map<String, String>
    ): Result<RemoteServerConfigPayload> {
        val params = session.parameters
        val hasAnyParam = params.any { entry -> entry.value.any { it.isNotBlank() } } ||
                files.any { (key, _) -> key != "postData" }

        val payloadSource = files["postData"]?.takeIf { it.isNotBlank() }?.let { raw ->
            val possibleFile = File(raw)
            if (possibleFile.exists()) {
                runCatching { possibleFile.readText() }.getOrNull()
            } else {
                raw
            }
        }
        if (!payloadSource.isNullOrBlank()) {
            val payload = runCatching {
                jsonDecoder.decodeFromString(RemoteServerConfigPayload.serializer(), payloadSource)
            }.getOrNull()
            if (payload != null) {
                return Result.success(payload)
            }
            if (!hasAnyParam) {
                return Result.failure(IllegalArgumentException("Invalid payload"))
            }
        }

        if (!hasAnyParam) {
            return Result.failure(IllegalArgumentException("Empty payload"))
        }

        val certificatePath = files["mtlsCertificate"]
        val certificateBytes = certificatePath?.let { path ->
            runCatching { File(path).takeIf { it.exists() }?.readBytes() }.getOrNull()
        }
        if (certificateBytes != null && certificateBytes.size > MAX_CERT_BYTES) {
            return Result.failure(IllegalArgumentException("Certificate too large"))
        }

        val mtlsCertificateName = params["mtlsCertificate"]?.firstOrNull()?.takeIf { it.isNotBlank() }
            ?: certificatePath?.let { File(it).name }
        val mtlsCertificateBase64 = certificateBytes?.let { Base64.getEncoder().encodeToString(it) }

        val payload = RemoteServerConfigPayload(
            connectionType = params["connectionType"]?.firstOrNull(),
            fullUrl = params["fullUrl"]?.firstOrNull(),
            mediaToken = params["mediaToken"]?.firstOrNull(),
            password = params["password"]?.firstOrNull(),
            quickConnectToken = params["quickConnectToken"]?.firstOrNull(),
            quickConnectUrl = params["quickConnectUrl"]?.firstOrNull(),
            port = params["port"]?.firstOrNull()?.toIntOrNull(),
            serverUrl = params["serverUrl"]?.firstOrNull(),
            mtlsEnabled = params["mtlsEnabled"]?.firstOrNull()?.equals("true", ignoreCase = true),
            mtlsCertificate = null,
            mtlsCertificateBase64 = mtlsCertificateBase64,
            mtlsCertificateName = mtlsCertificateName,
            mtlsCertificatePassword = params["mtlsCertificatePassword"]?.firstOrNull(),
            username = params["username"]?.firstOrNull()
        )

        return Result.success(payload)
    }
}