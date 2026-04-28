package com.example.skybuddy.core.permission

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class PermissionController(
    private val launch: (String) -> Unit
) {
    fun request(permission: String) = launch(permission)
}

@Composable
fun rememberPermissionController(
    onResult: (granted: Boolean) -> Unit
): PermissionController {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = onResult
    )
    return remember(launcher) { PermissionController { permission -> launcher.launch(permission) } }
}
