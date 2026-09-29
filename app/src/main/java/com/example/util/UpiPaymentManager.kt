package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.example.data.model.ProfileEntity

data class UpiAppInfo(
    val name: String,
    val packageName: String,
    val badge: String,
    val isInstalled: Boolean = false
)

object UpiPaymentManager {

    // NPCI & Indian UPI Merchant VPA defaults
    const val DEFAULT_MERCHANT_VPA = "vibesync@icici"
    const val DEFAULT_MERCHANT_NAME = "VibeSync India NPCI Payment"

    /**
     * Checks if the user is an Indian Registered User.
     * Checks user's country name ("India"), country flag ("🇮🇳"), or phone number (+91).
     */
    fun isIndianRegisteredUser(user: ProfileEntity?): Boolean {
        if (user == null) return false
        val countryMatch = user.country.equals("India", ignoreCase = true)
        val flagMatch = user.countryFlag.contains("🇮🇳") || user.countryFlag.contains("IN")
        return countryMatch || flagMatch
    }

    fun isIndianRegisteredUser(country: String, countryFlag: String, phone: String = ""): Boolean {
        val countryMatch = country.equals("India", ignoreCase = true)
        val flagMatch = countryFlag.contains("🇮🇳") || countryFlag.contains("IN")
        val phoneMatch = phone.startsWith("+91") || phone.startsWith("91")
        return countryMatch || flagMatch || phoneMatch
    }

    /**
     * List of supported Indian UPI Payment Apps with packages & badges
     */
    val SUPPORTED_UPI_APPS = listOf(
        UpiAppInfo("Google Pay", "com.google.android.apps.nbu.paisa.user", "🔵 GPay"),
        UpiAppInfo("PhonePe", "com.phonepe.app", "🟣 PhonePe"),
        UpiAppInfo("Paytm", "net.one97.paytm", "🩵 Paytm"),
        UpiAppInfo("BHIM UPI", "in.org.npci.upiapp", "🟠 BHIM"),
        UpiAppInfo("Mobikwik UPI", "com.mobikwik_new", "🔵 Mobikwik"),
        UpiAppInfo("CRED Pay", "com.dreamplug.androidapp", "🖤 CRED"),
        UpiAppInfo("Amazon Pay", "com.amazon.mShop.android.shopping", "🟠 Amazon")
    )

    /**
     * Query PackageManager to find installed UPI apps on user's Android phone
     */
    fun getInstalledUpiApps(context: Context): List<UpiAppInfo> {
        val pm = context.packageManager
        return SUPPORTED_UPI_APPS.map { app ->
            val isInstalled = try {
                pm.getPackageInfo(app.packageName, PackageManager.GET_ACTIVITIES)
                true
            } catch (e: Exception) {
                false
            }
            app.copy(isInstalled = isInstalled)
        }
    }

    /**
     * Generate standard NPCI compliant UPI Uri scheme
     * upi://pay?pa=VPA&pn=NAME&am=AMOUNT&cu=INR&tn=NOTE&tr=TXN_REF
     */
    fun generateUpiPayUri(
        vpa: String = DEFAULT_MERCHANT_VPA,
        payeeName: String = DEFAULT_MERCHANT_NAME,
        amount: String,
        note: String = "VibeSync Payment",
        txnRef: String = "TXN${System.currentTimeMillis()}"
    ): Uri {
        val cleanAmount = amount.replace("[^0-9.]".toRegex(), "").ifEmpty { "100" }
        val uriBuilder = Uri.Builder()
            .scheme("upi")
            .authority("pay")
            .appendQueryParameter("pa", vpa)
            .appendQueryParameter("pn", payeeName)
            .appendQueryParameter("mc", "0000")
            .appendQueryParameter("tr", txnRef)
            .appendQueryParameter("tn", note)
            .appendQueryParameter("am", cleanAmount)
            .appendQueryParameter("cu", "INR")
        return uriBuilder.build()
    }

    /**
     * Launch selected UPI App or general UPI intent launcher on Android
     */
    fun launchUpiAppIntent(context: Context, packageName: String?, uri: Uri): Intent {
        val intent = Intent(Intent.ACTION_VIEW, uri)
        if (!packageName.isNull_or_blank()) {
            intent.setPackage(packageName)
        }
        return intent
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()
