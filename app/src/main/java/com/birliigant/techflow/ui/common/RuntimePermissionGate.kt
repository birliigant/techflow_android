package com.birliigant.techflow.ui.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.birliigant.techflow.data.repository.UiPreferenceRepository

@Composable
fun RuntimePermissionGate(uiPreferenceRepository: UiPreferenceRepository) {
    val context = LocalContext.current
    val initialPermissions = remember { initialRuntimePermissions() }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        uiPreferenceRepository.markInitialRuntimePermissionsRequested()
    }

    LaunchedEffect(initialPermissions) {
        if (uiPreferenceRepository.hasRequestedInitialRuntimePermissions()) {
            return@LaunchedEffect
        }
        uiPreferenceRepository.markInitialRuntimePermissionsRequested()
        val missingPermissions = initialPermissions.filterNot(context::hasRuntimePermission)
        if (missingPermissions.isNotEmpty()) {
            launcher.launch(missingPermissions.toTypedArray())
        }
    }
}

fun imageReadRuntimePermissions(): Array<String> {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        else -> emptyArray()
    }
}

fun Context.hasRuntimePermissions(permissions: Array<String>): Boolean {
    return permissions.all(::hasRuntimePermission)
}

private fun initialRuntimePermissions(): List<String> {
    return buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

private fun Context.hasRuntimePermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}
