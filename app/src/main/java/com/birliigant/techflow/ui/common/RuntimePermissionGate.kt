package com.birliigant.techflow.ui.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.birliigant.techflow.data.repository.UiPreferenceRepository

@Composable
fun RuntimePermissionGate(uiPreferenceRepository: UiPreferenceRepository) {
    val context = LocalContext.current
    val initialPermissions = remember { initialRuntimePermissions() }
    var pendingInitialPermissions by remember { mutableStateOf<List<String>>(emptyList()) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        uiPreferenceRepository.markInitialRuntimePermissionsRequested()
    }

    LaunchedEffect(initialPermissions) {
        if (uiPreferenceRepository.hasRequestedInitialRuntimePermissions()) {
            return@LaunchedEffect
        }
        val missingPermissions = initialPermissions.filterNot(context::hasRuntimePermission)
        if (missingPermissions.isEmpty()) {
            uiPreferenceRepository.markInitialRuntimePermissionsRequested()
        } else {
            pendingInitialPermissions = missingPermissions
        }
    }

    if (pendingInitialPermissions.isNotEmpty()) {
        PermissionRationaleDialog(
            title = "需要一些权限",
            message = "TechFlow 需要 ${runtimePermissionLabels(pendingInitialPermissions)}，用于提供对应的系统能力。你可以稍后在系统设置中重新开启。",
            onDismiss = {
                uiPreferenceRepository.markInitialRuntimePermissionsRequested()
                pendingInitialPermissions = emptyList()
            },
            onConfirm = {
                val permissions = pendingInitialPermissions.toTypedArray()
                pendingInitialPermissions = emptyList()
                launcher.launch(permissions)
            },
        )
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

fun runtimePermissionLabels(permissions: Collection<String>): String {
    return permissions.map(::runtimePermissionLabel).distinct().joinToString("、")
}

@Composable
fun PermissionRationaleDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("去授权")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("暂不授权")
            }
        },
    )
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

private fun runtimePermissionLabel(permission: String): String {
    return when (permission) {
        Manifest.permission.POST_NOTIFICATIONS -> "通知权限"
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        -> "图片访问权限"
        else -> "必要权限"
    }
}
