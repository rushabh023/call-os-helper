package com.example.helper_application.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.helper_application.R
import com.example.helper_application.setup.SystemSettingsHelper
import com.example.helper_application.util.AppLog
import com.example.helper_application.ui.components.CubeGradientBackground
import com.example.helper_application.ui.components.CubePrimaryButton
import com.example.helper_application.ui.theme.CubePinkAccent
import com.example.helper_application.ui.theme.CubeTextOnGradient

@Composable
fun AppConnectorScreen(
    onContinue: () -> Unit,
    onResumeCheck: () -> Boolean
) {
    val context = LocalContext.current

    CubeGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(R.string.app_connector_intro),
                    color = CubeTextOnGradient,
                    fontSize = 17.sp,
                    lineHeight = 26.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.app_connector_warning),
                    color = CubeTextOnGradient,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                AccessibilityPreviewCard()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.app_connector_guide),
                    color = CubeTextOnGradient,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    Spacer(modifier = Modifier.height(12.dp))
                    CubePrimaryButton(
                        text = stringResource(R.string.allow_restricted_settings),
                        onClick = { SystemSettingsHelper.openRestrictedSettings(context) }
                    )
                }
            }
            Column {
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
                        val enabled = onResumeCheck() || SystemSettingsHelper.isAppConnectorEnabled(context)
                        AppLog.i("App Connector screen OK — connector enabled=$enabled")
                        if (enabled) {
                            onContinue()
                        } else {
                            AppLog.w("App Connector still off — reopening settings")
                            SystemSettingsHelper.openAccessibilitySettings(context)
                        }
                    }
                )
            }
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
