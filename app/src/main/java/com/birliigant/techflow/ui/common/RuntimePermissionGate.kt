package com.birliigant.techflow.ui.common

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
    var showNetworkPermissionDialog by remember { mutableStateOf(false) }
    var showRuntimePermissionDialog by remember { mutableStateOf(false) }
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
        pendingInitialPermissions = missingPermissions
        showNetworkPermissionDialog = true
    }

    if (showNetworkPermissionDialog) {
        PermissionRationaleDialog(
            title = "网络权限",
            message = "允许 TechFlow 访问网络？",
            confirmText = "允许",
            dismissText = "退出",
            onDismiss = {
                context.findActivity()?.finish()
            },
            onConfirm = {
                showNetworkPermissionDialog = false
                if (pendingInitialPermissions.isEmpty()) {
                    uiPreferenceRepository.markInitialRuntimePermissionsRequested()
                } else {
                    showRuntimePermissionDialog = true
                }
            },
        )
    }

    if (showRuntimePermissionDialog) {
        PermissionRationaleDialog(
            title = "其他权限",
            message = "是否授权 ${runtimePermissionLabels(pendingInitialPermissions)}？",
            confirmText = "去授权",
            onDismiss = {
                uiPreferenceRepository.markInitialRuntimePermissionsRequested()
                showRuntimePermissionDialog = false
                pendingInitialPermissions = emptyList()
            },
            onConfirm = {
                val permissions = pendingInitialPermissions.toTypedArray()
                showRuntimePermissionDialog = false
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
    confirmText: String = "去授权",
    dismissText: String = "暂不授权",
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        },
    )
}

private fun initialRuntimePermissions(): List<String> {
    return buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
        addAll(imageReadRuntimePermissions())
    }
}

private fun Context.hasRuntimePermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
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
