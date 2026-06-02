package com.example.helper_application.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.helper_application.R
import com.example.helper_application.setup.SystemSettingsHelper
import com.example.helper_application.ui.components.CubeGradientBackground
import com.example.helper_application.ui.components.CubePrimaryButton
import com.example.helper_application.ui.components.SetupResponsive
import com.example.helper_application.ui.components.SetupScreenLogo
import com.example.helper_application.ui.components.SetupScrollableColumn
import com.example.helper_application.ui.theme.CubePinkAccent
import com.example.helper_application.ui.theme.CubeTextOnGradient
import com.example.helper_application.util.AppLog

@Composable
fun AppConnectorScreen(
    onContinue: () -> Unit,
    onResumeCheck: () -> Boolean
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val config = LocalConfiguration.current
    val gap = SetupResponsive.sectionGap(config.screenHeightDp)
    var connectorEnabled by remember { mutableStateOf(SystemSettingsHelper.isAppConnectorEnabled(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                connectorEnabled = SystemSettingsHelper.isAppConnectorEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    CubeGradientBackground {
        SetupScrollableColumn {
            SetupScreenLogo()
            Spacer(modifier = Modifier.height(gap))
            Text(
                text = stringResource(R.string.app_connector_intro),
                modifier = Modifier.fillMaxWidth(),
                color = CubeTextOnGradient,
                fontSize = 17.sp,
                lineHeight = 26.sp
            )
            Spacer(modifier = Modifier.height(gap))
            Text(
                text = stringResource(R.string.app_connector_warning),
                color = CubeTextOnGradient,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 22.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(gap + 8.dp))
            AccessibilityPreviewCard()
            Spacer(modifier = Modifier.height(gap))
            Text(
                text = stringResource(R.string.app_connector_guide),
                color = CubeTextOnGradient,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(gap))
            Text(
                text = if (connectorEnabled) {
                    stringResource(R.string.app_connector_status_on)
                } else {
                    stringResource(R.string.app_connector_status_off)
                },
                color = if (connectorEnabled) Color(0xFFB9F6CA) else Color(0xFFFFCDD2),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            if (!connectorEnabled) {
                Spacer(modifier = Modifier.height(gap))
                Text(
                    text = stringResource(R.string.app_connector_realme_hint),
                    color = CubeTextOnGradient.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                Spacer(modifier = Modifier.height(gap + 4.dp))
                CubePrimaryButton(
                    text = stringResource(R.string.allow_restricted_settings),
                    onClick = { SystemSettingsHelper.openRestrictedSettings(context) }
                )
            }
            Spacer(modifier = Modifier.height(gap + 8.dp))
            CubePrimaryButton(
                text = stringResource(R.string.enable_app_connector),
                onClick = {
                    AppLog.i("Opening Accessibility settings (ENABLE APP CONNECTOR)")
                    SystemSettingsHelper.openAccessibilitySettings(context)
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            CubePrimaryButton(
                text = stringResource(R.string.ok),
                onClick = {
                    connectorEnabled = onResumeCheck() || SystemSettingsHelper.isAppConnectorEnabled(context)
                    AppLog.i("App Connector screen OK — connector enabled=$connectorEnabled")
                    if (connectorEnabled) {
                        onContinue()
                    } else {
                        AppLog.w("App Connector still off — reopening settings")
                        SystemSettingsHelper.openAccessibilitySettings(context)
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AccessibilityPreviewCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text("Accessibility", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Downloaded apps", fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.app_connector_name),
            modifier = Modifier
                .fillMaxWidth()
                .background(CubePinkAccent.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                .padding(10.dp),
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
        Text("Off", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
    }
}
