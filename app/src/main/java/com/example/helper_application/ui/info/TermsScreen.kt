package com.example.helper_application.ui.info

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.helper_application.R
import com.example.helper_application.ui.theme.CubeDashboardBackground
import com.example.helper_application.ui.theme.CubePinkAccent

@Composable
fun TermsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CubeDashboardBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.terms_title),
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = CubePinkAccent
        )
        Spacer(modifier = Modifier.height(12.dp))
        TermsSection(title = stringResource(R.string.terms_section_recording_law), body = stringResource(R.string.terms_body_recording_law))
        TermsSection(title = stringResource(R.string.terms_section_no_warranty), body = stringResource(R.string.terms_body_no_warranty))
        TermsSection(title = stringResource(R.string.terms_section_device_compat), body = stringResource(R.string.terms_body_device_compat))
        TermsSection(title = stringResource(R.string.terms_section_if_not_working), body = stringResource(R.string.terms_body_if_not_working))
        TermsSection(title = stringResource(R.string.terms_section_shizuku), body = stringResource(R.string.terms_body_shizuku))
        TermsSection(title = stringResource(R.string.terms_section_root), body = stringResource(R.string.terms_body_root))
        TermsSection(title = stringResource(R.string.terms_section_privacy), body = stringResource(R.string.terms_body_privacy))
        TermsSection(title = stringResource(R.string.terms_section_acceptance), body = stringResource(R.string.terms_body_acceptance))
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun TermsSection(title: String, body: String) {
    Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(vertical = 8.dp))
    Text(text = body, fontSize = 14.sp, lineHeight = 21.sp, color = Color.DarkGray)
    Spacer(modifier = Modifier.height(8.dp))
}
