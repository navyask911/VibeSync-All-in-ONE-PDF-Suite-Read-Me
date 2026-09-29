package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Type of network interface currently active.
 */
enum class NetworkType {
    WIFI,
    CELLULAR,
    ETHERNET,
    VPN,
    NONE,
    UNKNOWN
}

/**
 * Firestore Sync status tied to network availability.
 */
enum class FirestoreSyncState {
    SYNC_ACTIVE,
    SYNC_PAUSED_OFFLINE,
    SYNC_RECONNECTING,
    SYNC_RESTORED
}

/**
 * Complete immutable snapshot of device network state.
 */
data class NetworkState(
    val isConnected: Boolean = true,
    val networkType: NetworkType = NetworkType.UNKNOWN,
    val syncState: FirestoreSyncState = FirestoreSyncState.SYNC_ACTIVE,
    val isMetered: Boolean = false,
    val lastChangedTimestamp: Long = System.currentTimeMillis(),
    val offlineDurationSeconds: Long = 0L,
    val message: String = "Connected to network. Real-time Firestore sync active."
) {
    val formattedLastChanged: String
        get() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastChangedTimestamp))

    val isSyncPaused: Boolean
        get() = syncState == FirestoreSyncState.SYNC_PAUSED_OFFLINE || !isConnected
}

/**
 * NetworkMonitor
 *
 * Singleton utility that tracks real-time network connectivity using Android's ConnectivityManager.
 * Notifies the UI and ViewModels when connection drops so users are informed that Firestore
 * real-time synchronization is temporarily paused and queuing offline.
 */
class NetworkMonitor private constructor() {

    companion object {
        private const val TAG = "NetworkMonitor"

        @Volatile
        private var instance: NetworkMonitor? = null

        fun getInstance(): NetworkMonitor {
            return instance ?: synchronized(this) {
                instance ?: NetworkMonitor().also { instance = it }
            }
        }
    }

    private val monitorScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var isMonitoring = false

    private var disconnectedAtTimestamp: Long = 0L

    private val _networkState = MutableStateFlow(NetworkState(isConnected = true, syncState = FirestoreSyncState.SYNC_ACTIVE))
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()

    /**
     * Starts listening to system network changes via ConnectivityManager.NetworkCallback.
     */
    fun startMonitoring(context: Context) {
        if (isMonitoring) return

        try {
            val appContext = context.applicationContext
            connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

            if (connectivityManager == null) {
                Log.e(TAG, "ConnectivityManager is unavailable on this device.")
                return
            }

            // Initial immediate check
            val initialConnected = checkCurrentConnectivity()
            val initialType = getCurrentNetworkType()
            _networkState.value = NetworkState(
                isConnected = initialConnected,
                networkType = initialType,
                syncState = if (initialConnected) FirestoreSyncState.SYNC_ACTIVE else FirestoreSyncState.SYNC_PAUSED_OFFLINE,
                message = if (initialConnected) "Online: Syncing with Firestore" else "Offline: Firestore sync paused (Local caching active)"
            )

            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    val wasOffline = !_networkState.value.isConnected
                    val now = System.currentTimeMillis()
                    val offlineSeconds = if (wasOffline && disconnectedAtTimestamp > 0) {
                        (now - disconnectedAtTimestamp) / 1000
                    } else 0L

                    val type = getCurrentNetworkType()
                    Log.i(TAG, "🌐 [NETWORK AVAILABLE] Interface: $type | Previously offline for ${offlineSeconds}s")

                    monitorScope.launch {
                        _networkState.value = NetworkState(
                            isConnected = true,
                            networkType = type,
                            syncState = if (wasOffline) FirestoreSyncState.SYNC_RESTORED else FirestoreSyncState.SYNC_ACTIVE,
                            lastChangedTimestamp = now,
                            offlineDurationSeconds = offlineSeconds,
                            message = if (wasOffline) {
                                "Connection restored! Resuming Firestore real-time synchronization..."
                            } else {
                                "Connected via $type. Real-time Firestore sync active."
                            }
                        )
                    }
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    val isMetered = !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                    val type = when {
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkType.VPN
                        else -> NetworkType.UNKNOWN
                    }

                    monitorScope.launch {
                        _networkState.value = _networkState.value.copy(
                            isConnected = hasInternet,
                            networkType = type,
                            isMetered = isMetered,
                            syncState = if (hasInternet) FirestoreSyncState.SYNC_ACTIVE else FirestoreSyncState.SYNC_PAUSED_OFFLINE
                        )
                    }
                }

                override fun onLost(network: Network) {
                    val now = System.currentTimeMillis()
                    disconnectedAtTimestamp = now
                    Log.w(TAG, "🔌 [NETWORK LOST] Device is offline. Pausing Firestore sync & enabling local cache.")

                    monitorScope.launch {
                        _networkState.value = NetworkState(
                            isConnected = false,
                            networkType = NetworkType.NONE,
                            syncState = FirestoreSyncState.SYNC_PAUSED_OFFLINE,
                            lastChangedTimestamp = now,
                            message = "Network connection lost. Firestore sync is paused; changes will auto-sync when online."
                        )
                    }
                }

                override fun onUnavailable() {
                    val now = System.currentTimeMillis()
                    disconnectedAtTimestamp = now
                    Log.w(TAG, "⚠️ [NETWORK UNAVAILABLE] No internet route available.")

                    monitorScope.launch {
                        _networkState.value = NetworkState(
                            isConnected = false,
                            networkType = NetworkType.NONE,
                            syncState = FirestoreSyncState.SYNC_PAUSED_OFFLINE,
                            lastChangedTimestamp = now,
                            message = "Network unavailable. Firestore sync is paused."
                        )
                    }
                }
            }

            networkCallback?.let {
                connectivityManager?.registerNetworkCallback(request, it)
                isMonitoring = true
                Log.i(TAG, "✅ NetworkMonitor successfully registered with ConnectivityManager.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting NetworkMonitor: ${e.message}", e)
        }
    }

    /**
     * Checks whether the device is actively connected to an internet-capable network right now.
     */
    fun checkCurrentConnectivity(): Boolean {
        return try {
            val cm = connectivityManager ?: return true
            val activeNetwork = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            Log.w(TAG, "Error checking connectivity: ${e.message}")
            true
        }
    }

    /**
     * Resolves the active network transport type (WIFI, Cellular, Ethernet, VPN, etc.).
     */
    fun getCurrentNetworkType(): NetworkType {
        return try {
            val cm = connectivityManager ?: return NetworkType.UNKNOWN
            val activeNetwork = cm.activeNetwork ?: return NetworkType.NONE
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return NetworkType.NONE
            when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
                caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkType.VPN
                else -> NetworkType.UNKNOWN
            }
        } catch (_: Exception) {
            NetworkType.UNKNOWN
        }
    }

    /**
     * Unregisters the network callback and resets state.
     */
    fun stopMonitoring() {
        try {
            networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
            networkCallback = null
            isMonitoring = false
            Log.i(TAG, "🛑 NetworkMonitor stopped.")
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping NetworkMonitor: ${e.message}")
        }
    }
}
