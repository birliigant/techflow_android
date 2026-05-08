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
    var showInitialPermissionDialog by remember { mutableStateOf(false) }
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
        showInitialPermissionDialog = true
    }

    if (showInitialPermissionDialog) {
        PermissionRationaleDialog(
            title = "权限说明",
            message = initialPermissionMessage(context, pendingInitialPermissions),
            confirmText = if (pendingInitialPermissions.isEmpty()) "知道了" else "去授权",
            onDismiss = {
                uiPreferenceRepository.markInitialRuntimePermissionsRequested()
                showInitialPermissionDialog = false
                pendingInitialPermissions = emptyList()
            },
            onConfirm = {
                val permissions = pendingInitialPermissions.toTypedArray()
                showInitialPermissionDialog = false
                pendingInitialPermissions = emptyList()
                if (permissions.isEmpty()) {
                    uiPreferenceRepository.markInitialRuntimePermissionsRequested()
                } else {
                    launcher.launch(permissions)
                }
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
        addAll(imageReadRuntimePermissions())
    }
}

private fun initialPermissionMessage(
    context: Context,
    missingRuntimePermissions: List<String>,
): String {
    val networkStatus = if (context.hasRuntimePermission(Manifest.permission.INTERNET)) {
        "已授权"
    } else {
        "未授权，请检查安装包权限声明"
    }
    val runtimeMessage = if (missingRuntimePermissions.isEmpty()) {
        "当前没有需要弹出系统授权框的运行时权限。"
    } else {
        "接下来会申请 ${runtimePermissionLabels(missingRuntimePermissions)}，用于通知提醒、图片上传等功能。"
    }
    return "网络访问权限：$networkStatus。Android 将网络访问视为普通权限，安装时自动授予，不提供运行时授权弹窗。\n\n$runtimeMessage"
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
