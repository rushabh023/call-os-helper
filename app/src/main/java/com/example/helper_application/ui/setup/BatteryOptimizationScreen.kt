package com.example.helper_application.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
fun BatteryOptimizationScreen(onContinue: () -> Unit) {
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
                    text = stringResource(R.string.battery_title),
                    color = CubeTextOnGradient,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.battery_message),
                    color = CubeTextOnGradient,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                )
                Spacer(modifier = Modifier.height(32.dp))
                BatterySettingsCard()
            }
            CubePrimaryButton(
                text = stringResource(R.string.ok),
                onClick = {
                    AppLog.i("Battery screen OK — opening battery settings")
                    SystemSettingsHelper.openBatteryOptimization(context)
                    onContinue()
                }
            )
        }
    }
}

@Composable
private fun BatterySettingsCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.app_name),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Manage battery usage",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.battery_allow_background))
            Text("On", color = CubePinkAccent, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.battery_not_optimized))
            Text(
                stringResource(R.string.battery_not_optimized_value),
                color = CubePinkAccent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
