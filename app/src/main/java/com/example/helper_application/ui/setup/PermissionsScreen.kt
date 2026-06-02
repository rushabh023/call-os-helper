package com.example.helper_application.ui.setup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.helper_application.R
import com.example.helper_application.setup.PermissionHelper
import com.example.helper_application.ui.components.CubeGradientBackground
import com.example.helper_application.ui.components.CubePrimaryButton
import com.example.helper_application.ui.components.SetupResponsive
import com.example.helper_application.ui.components.SetupScreenLogo
import com.example.helper_application.ui.components.SetupScrollableColumn
import com.example.helper_application.ui.theme.CubeTextOnGradient
import com.example.helper_application.util.AppLog

@Composable
fun PermissionsScreen(onPermissionsGranted: () -> Unit) {
    val config = LocalConfiguration.current
    val gap = SetupResponsive.sectionGap(config.screenHeightDp)
    val rowGap = if (config.screenHeightDp < 600) 14.dp else 20.dp

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        result.forEach { (perm, granted) ->
            AppLog.d("Permission $perm → ${if (granted) "GRANTED" else "DENIED"}")
        }
        if (result.values.all { it }) {
            AppLog.i("All runtime permissions granted")
            onPermissionsGranted()
        } else {
            AppLog.w("Some permissions denied — setup blocked until all granted")
        }
    }

    CubeGradientBackground {
        SetupScrollableColumn {
            SetupScreenLogo(large = true)
            Spacer(modifier = Modifier.height(gap + 4.dp))
            Text(
                text = stringResource(R.string.permissions_title),
                color = CubeTextOnGradient,
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 28.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(if (config.screenHeightDp < 600) 24.dp else 36.dp))
            PermissionRow(Icons.Default.Mic, R.string.permission_mic, R.string.permission_mic_desc)
            Spacer(modifier = Modifier.height(rowGap))
            PermissionRow(Icons.Default.Phone, R.string.permission_phone, R.string.permission_phone_desc)
            Spacer(modifier = Modifier.height(rowGap))
            PermissionRow(Icons.Default.Contacts, R.string.permission_call_log, R.string.permission_call_log_desc)
            Spacer(modifier = Modifier.height(rowGap))
            PermissionRow(Icons.Default.Bluetooth, R.string.permission_bluetooth, R.string.permission_bluetooth_desc)
            Spacer(modifier = Modifier.height(gap + 12.dp))
            CubePrimaryButton(
                text = stringResource(R.string.grant_permissions),
                onClick = {
                    AppLog.i("User tapped GRANT PERMISSIONS")
                    permissionLauncher.launch(PermissionHelper.requiredRuntimePermissions())
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    titleRes: Int,
    descRes: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CubeTextOnGradient,
            modifier = Modifier.size(28.dp)
        )
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(
                text = stringResource(titleRes),
                color = CubeTextOnGradient,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            )
            Text(
                text = stringResource(descRes),
                color = CubeTextOnGradient.copy(alpha = 0.9f),
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }
    }
}
