package com.example.helper_application.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.helper_application.R
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            content()
        }
    }
}

/** Responsive padding and logo size for setup screens (phones, tablets, small height). */
object SetupResponsive {
    fun horizontalPadding(screenWidthDp: Int): Dp = when {
        screenWidthDp < 340 -> 14.dp
        screenWidthDp < 400 -> 20.dp
        screenWidthDp < 600 -> 24.dp
        else -> 32.dp
    }

    fun logoSize(screenHeightDp: Int, large: Boolean = false): Dp = when {
        screenHeightDp < 520 -> if (large) 64.dp else 52.dp
        screenHeightDp < 640 -> if (large) 76.dp else 64.dp
        screenHeightDp < 800 -> if (large) 88.dp else 72.dp
        else -> if (large) 96.dp else 80.dp
    }

    fun sectionGap(screenHeightDp: Int): Dp = if (screenHeightDp < 600) 12.dp else 16.dp
}

/**
 * Full-height scrollable column for setup wizards — content + action buttons stay reachable
 * on small screens (e.g. Realme 720×1600) and large fonts.
 */
@Composable
fun SetupScrollableColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val config = LocalConfiguration.current
    val horizontal = SetupResponsive.horizontalPadding(config.screenWidthDp)
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = horizontal)
            .padding(top = 12.dp, bottom = 28.dp)
    ) {
        content()
    }
}

@Composable
fun SetupScreenLogo(large: Boolean = false) {
    val config = LocalConfiguration.current
    val logoSize = SetupResponsive.logoSize(config.screenHeightDp, large)
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        AppLogo(size = logoSize)
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
fun AppLogo(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    /** On purple headers use foreground-only; setup screens use full logo with gradient. */
    foregroundOnly: Boolean = false
) {
    Image(
        painter = painterResource(
            if (foregroundOnly) R.drawable.ic_launcher_foreground else R.drawable.ic_app_logo
        ),
        contentDescription = null,
        modifier = modifier.size(size)
    )
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
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppLogo(size = 40.dp, foregroundOnly = true)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                color = CubeTextOnGradient,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }
    }
}
