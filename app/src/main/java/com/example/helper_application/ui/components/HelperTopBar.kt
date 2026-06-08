package com.example.helper_application.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.helper_application.ui.theme.CubeHeaderPurple
import com.example.helper_application.ui.theme.CubePurple
import com.example.helper_application.ui.theme.CubeTextOnGradient

@Composable
fun HelperTopBar(
    title: String,
    onMenuClick: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(CubeHeaderPurple, CubePurple)
                )
            )
            .padding(horizontal = 4.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            when {
                onBackClick != null -> {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = CubeTextOnGradient)
                    }
                }
                onMenuClick != null -> {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Filled.Menu, contentDescription = null, tint = CubeTextOnGradient)
                    }
                }
            }
            if (onMenuClick != null || onBackClick == null) {
                AppLogo(size = 36.dp, foregroundOnly = true)
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                text = title,
                color = CubeTextOnGradient,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }
    }
}
