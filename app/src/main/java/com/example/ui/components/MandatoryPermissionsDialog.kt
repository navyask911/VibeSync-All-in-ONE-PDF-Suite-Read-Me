package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun MandatoryPermissionsDialog(
    onPermissionsGranted: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Build platform-specific permissions list
    val permissionsToRequest = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.READ_CONTACTS)
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_IMAGES)
                add(Manifest.permission.READ_MEDIA_VIDEO)
                add(Manifest.permission.READ_MEDIA_AUDIO)
                add(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                add(Manifest.permission.READ_PHONE_NUMBERS)
            }
            add(Manifest.permission.READ_PHONE_STATE)
        }.toTypedArray()
    }

    val permissionsStatus = remember {
        mutableStateMapOf<String, Boolean>().apply {
            permissionsToRequest.forEach { perm ->
                put(perm, ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED)
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        results.forEach { (perm, granted) ->
            permissionsStatus[perm] = granted
        }
        val allGranted = results.values.all { it }
        onPermissionsGranted(allGranted)
        onDismiss()
    }

    LaunchedEffect(Unit) {
        launcher.launch(permissionsToRequest)
    }

    // Transparent loading state or blank indicator to bypass full page details
    Box(modifier = Modifier.size(1.dp).testTag("dialog_mandatory_permissions"))
}
