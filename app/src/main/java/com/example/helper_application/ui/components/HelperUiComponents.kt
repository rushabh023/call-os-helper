package com.example.helper_application.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.helper_application.ui.theme.CubeGrayButton
import com.example.helper_application.ui.theme.CubeHeaderPurple
import com.example.helper_application.ui.theme.CubePurple
import com.example.helper_application.ui.theme.CubeTextOnGradient

@Composable
fun CubeGradientBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1976D2),
                        Color(0xFF512DA8),
                        Color(0xFF6A1B9A)
                    )
                )
            )
    ) {
        content()
    }
}

@Composable
fun CubePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.35f),
            contentColor = CubeTextOnGradient,
            disabledContainerColor = Color.White.copy(alpha = 0.15f)
        )
    ) {
        Text(text = text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
fun CubeDashboardGrayButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = CubeGrayButton,
            contentColor = Color.Black.copy(alpha = 0.7f)
        )
    ) {
        Text(text = text, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

@Composable
fun CubePurpleHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(CubeHeaderPurple, CubePurple)
                )
            )
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Text(
            text = title,
            color = CubeTextOnGradient,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp
        )
    }
}
