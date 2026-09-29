package com.example.util

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * CoilImageLoaderConfig
 *
 * Configures the Coil image loading library with a custom OkHttpClient that:
 * 1. Integrates the Cloudflare R2 Base URL (`CLOUDFLARE_R2_BASE_URL`).
 * 2. Rewrites relative media asset paths (`/media/`, `/uploads/`, `/r2/`) to Cloudflare R2 CDN links.
 * 3. Sets a Cache-Control Interceptor enforcing 1-year client-side caching (`public, max-age=31536000`)
 *    and 30-day offline stale cache policy for all media assets to minimize network bandwidth consumption.
 */
object CoilImageLoaderConfig {

    const val CLOUDFLARE_R2_BASE_URL = "https://cdn.vibesync.app"
    private const val R2_DISK_CACHE_DIR_NAME = "cloudflare_r2_image_cache"
    private const val R2_DISK_CACHE_MAX_SIZE_BYTES = 100L * 1024 * 1024 // 100 MB Coil Disk Cache
    private const val OKHTTP_DISK_CACHE_MAX_SIZE_BYTES = 50L * 1024 * 1024 // 50 MB OkHttp Network Cache

    /**
     * Builds custom OkHttpClient integrated with Cloudflare R2 Base URL mapping
     * and a Cache-Control Interceptor enforcing aggressive client-side caching for all media assets.
     */
    fun createOkHttpClient(context: Context): OkHttpClient {
        val cacheDir = File(context.cacheDir, "okhttp_r2_cache")
        if (!cacheDir.exists()) cacheDir.mkdirs()

        return OkHttpClient.Builder()
            .cache(Cache(cacheDir, OKHTTP_DISK_CACHE_MAX_SIZE_BYTES))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                var request = chain.request()
                val urlString = request.url.toString()

                // Interceptor: Rewrite relative media paths to Cloudflare R2 Base URL automatically
                if (urlString.startsWith("/media/") || urlString.startsWith("/uploads/") || urlString.startsWith("/r2/")) {
                    val fullR2Url = "$CLOUDFLARE_R2_BASE_URL$urlString"
                    request = request.newBuilder().url(fullR2Url).build()
                }

                chain.proceed(request)
            }
            .addNetworkInterceptor { chain ->
                val originalResponse = chain.proceed(chain.request())
                val url = chain.request().url.toString()

                // Enforce Cache-Control header for all media assets (Images, Videos, Audio, Avatars)
                if (url.contains("r2") || url.contains("cloudflare") || url.contains("vibesync") ||
                    url.contains("firebasestorage") || url.contains(".jpg") || url.contains(".png") ||
                    url.contains(".webp") || url.contains(".mp4")
                ) {
                    originalResponse.newBuilder()
                        .header("Cache-Control", "public, max-age=31536000, max-stale=2592000") // 1 Year Edge Cache + 30-Day Offline Stale
                        .removeHeader("Pragma")
                        .build()
                } else {
                    originalResponse
                }
            }
            .build()
    }

    /**
     * Builds the global Coil ImageLoader configured with custom OkHttpClient, MemoryCache, and DiskCache.
     */
    fun createImageLoader(context: Context): ImageLoader {
        val customOkHttpClient = createOkHttpClient(context)

        return ImageLoader.Builder(context)
            .okHttpClient(customOkHttpClient)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25) // Allocates up to 25% of app memory for in-memory bitmap cache
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(context.cacheDir, R2_DISK_CACHE_DIR_NAME))
                    .maxSizeBytes(R2_DISK_CACHE_MAX_SIZE_BYTES)
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .crossfade(250)
            .build()
    }
}
