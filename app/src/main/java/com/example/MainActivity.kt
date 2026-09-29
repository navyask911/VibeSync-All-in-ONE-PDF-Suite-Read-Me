package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.example.ui.DatingAppRoot
import com.example.ui.theme.MyApplicationTheme
import com.example.util.AppNotificationManager

class MainActivity : FragmentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    enableEdgeToEdge()

    // Initialize Notification Channels and fetch FCM Registration Token safely
    AppNotificationManager.initChannels(this)
    AppNotificationManager.fetchFcmToken(this)

    setContent {
      MyApplicationTheme {
        DatingAppRoot()
      }
    }
  }

  override fun onResume() {
    super.onResume()
    AppNotificationManager.setAppForegroundState(true)
  }

  override fun onPause() {
    super.onPause()
    AppNotificationManager.setAppForegroundState(false)
  }

  override fun onStop() {
    super.onStop()
    AppNotificationManager.setAppForegroundState(false)
  }
}

