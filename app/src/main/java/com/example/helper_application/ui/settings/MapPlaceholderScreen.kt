package com.example.helper_application.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.helper_application.R
import com.example.helper_application.ui.theme.CubeDashboardBackground

@Composable
fun MapPlaceholderScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CubeDashboardBackground),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.map_placeholder),
            fontSize = 15.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(32.dp)
        )
    }
}
