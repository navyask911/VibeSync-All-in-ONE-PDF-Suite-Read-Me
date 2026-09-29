package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.URL

object AppUpdateHelper {

    private const val TAG = "AppUpdateHelper"
    private var wifiServerRunning = false
    private var serverSocket: ServerSocket? = null

    /**
     * Extracts the installed base APK of this app and creates a shareable copy.
     */
    fun getInstalledApkFile(context: Context): File? {
        return try {
            val sourceApk = File(context.applicationInfo.sourceDir)
            if (!sourceApk.exists()) return null

            val cacheDir = File(context.cacheDir, "apk_export")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionName = pInfo.versionName ?: "1.0"
            val targetApk = File(cacheDir, "VibeSync_v${versionName}.apk")

            // Copy source APK to cache so FileProvider can safely share it
            sourceApk.inputStream().use { input ->
                targetApk.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            targetApk
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to export installed APK: ${e.message}")
            null
        }
    }

    /**
     * Shares the current installed APK directly to another phone via VibeSync, Quick Share, Bluetooth, etc.
     */
    fun shareInstalledApk(context: Context): Boolean {
        return try {
            val apkFile = getInstalledApkFile(context) ?: return false
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "VibeSync Latest Version APK")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Here is the latest VibeSync app APK! Tap the file to install directly without any USB cable."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Send VibeSync APK to Phone")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to share installed APK: ${e.message}")
            false
        }
    }

    /**
     * Launches the native Android package installer for a given APK file.
     */
    fun installApk(context: Context, apkFile: File): Boolean {
        return try {
            if (!apkFile.exists()) return false

            // On Android 8.0+, verify if package install permission is granted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                    return false
                }
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
            true
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to install APK: ${e.message}")
            false
        }
    }

    /**
     * Downloads an APK from an internet/cloud URL and installs it.
     */
    suspend fun downloadApkFromUrl(
        context: Context,
        downloadUrl: String,
        onProgress: (Int) -> Unit,
        onSuccess: (File) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val url = URL(downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                withContext(Dispatchers.Main) {
                    onError("Server returned HTTP ${connection.responseCode} ${connection.responseMessage}")
                }
                return@withContext
            }

            val fileLength = connection.contentLength
            val downloadDir = File(context.cacheDir, "apk_downloads")
            if (!downloadDir.exists()) downloadDir.mkdirs()

            val outputFile = File(downloadDir, "VibeSync_Update.apk")
            if (outputFile.exists()) outputFile.delete()

            val input = BufferedInputStream(connection.inputStream)
            val output = FileOutputStream(outputFile)

            val data = ByteArray(4096)
            var total: Long = 0
            var count: Int

            while (input.read(data).also { count = it } != -1) {
                total += count
                if (fileLength > 0) {
                    val progress = (total * 100 / fileLength).toInt()
                    withContext(Dispatchers.Main) { onProgress(progress) }
                }
                output.write(data, 0, count)
            }

            output.flush()
            output.close()
            input.close()

            withContext(Dispatchers.Main) {
                onSuccess(outputFile)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError("Download error: ${e.message ?: "Failed to connect"}")
            }
        }
    }

    /**
     * Gets the device's local IPv4 address on Wi-Fi or mobile hotspot.
     */
    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress
                        if (host != null && host.contains(".")) {
                            return host
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return "192.168.43.1" // Common hotspot default
    }

    /**
     * Starts a lightweight local HTTP server on port 8888 to stream the installed APK to another phone.
     */
    suspend fun startLocalApkServer(
        context: Context,
        port: Int = 8888,
        onStarted: (String) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (wifiServerRunning) {
            val ip = getLocalIpAddress()
            withContext(Dispatchers.Main) { onStarted("http://$ip:$port/vibesync.apk") }
            return@withContext
        }

        try {
            val apkFile = getInstalledApkFile(context)
            if (apkFile == null || !apkFile.exists()) {
                withContext(Dispatchers.Main) { onError("Could not extract local APK") }
                return@withContext
            }

            val socket = ServerSocket(port)
            serverSocket = socket
            wifiServerRunning = true

            val ip = getLocalIpAddress()
            val serverUrl = "http://$ip:$port/vibesync.apk"
            withContext(Dispatchers.Main) { onStarted(serverUrl) }

            while (wifiServerRunning && !socket.isClosed) {
                try {
                    val client = socket.accept()
                    // Handle client in background
                    Thread {
                        try {
                            val inStream = BufferedReader(InputStreamReader(client.getInputStream()))
                            val outStream = client.getOutputStream()
                            var line: String? = inStream.readLine()
                            while (!line.isNullOrBlank()) {
                                line = inStream.readLine()
                            }

                            val header = "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: application/vnd.android.package-archive\r\n" +
                                    "Content-Length: ${apkFile.length()}\r\n" +
                                    "Content-Disposition: attachment; filename=\"VibeSync_Latest.apk\"\r\n" +
                                    "Connection: close\r\n\r\n"
                            outStream.write(header.toByteArray())

                            apkFile.inputStream().use { fileInput ->
                                fileInput.copyTo(outStream)
                            }
                            outStream.flush()
                            client.close()
                        } catch (_: Exception) {}
                    }.start()
                } catch (_: Exception) {
                    break
                }
            }
        } catch (e: Exception) {
            wifiServerRunning = false
            withContext(Dispatchers.Main) { onError("Failed to start server: ${e.message}") }
        }
    }

    fun stopLocalApkServer() {
        wifiServerRunning = false
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
    }

    fun isLocalServerRunning(): Boolean = wifiServerRunning
}
