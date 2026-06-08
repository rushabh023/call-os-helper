package com.example.helper_application.ui.info

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.helper_application.R
import com.example.helper_application.shizuku.ShizukuManager
import com.example.helper_application.ui.theme.CubeDashboardBackground
import com.example.helper_application.ui.theme.CubePinkAccent

@Composable
fun FaqScreen(onOpenShizukuGuide: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CubeDashboardBackground)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = stringResource(R.string.faq_intro),
            modifier = Modifier.padding(16.dp),
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = Color.DarkGray
        )
        FaqItem(
            question = stringResource(R.string.faq_q_work_all_devices),
            answer = stringResource(R.string.faq_a_work_all_devices)
        )
        FaqItem(
            question = stringResource(R.string.faq_q_good_quality),
            answer = stringResource(R.string.faq_a_good_quality)
        )
        FaqItem(
            question = stringResource(R.string.faq_q_only_my_voice),
            answer = stringResource(R.string.faq_a_only_my_voice)
        )
        FaqItem(
            question = stringResource(R.string.faq_q_other_voice_low),
            answer = stringResource(R.string.faq_a_other_voice_low)
        )
        FaqItem(
            question = stringResource(R.string.faq_q_shizuku),
            answer = stringResource(R.string.faq_a_shizuku)
        )
        FaqItem(
            question = stringResource(R.string.faq_q_root),
            answer = stringResource(R.string.faq_a_root)
        )
        FaqItem(
            question = stringResource(R.string.faq_q_samsung),
            answer = stringResource(R.string.faq_a_samsung)
        )
        FaqItem(
            question = stringResource(R.string.faq_q_legal),
            answer = stringResource(R.string.faq_a_legal)
        )
        TextButton(
            onClick = { ShizukuManager.openSetupGuide(context) },
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text(stringResource(R.string.faq_open_shizuku_guide), color = CubePinkAccent, fontWeight = FontWeight.Bold)
        }
        TextButton(
            onClick = onOpenShizukuGuide,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text(stringResource(R.string.faq_open_helper_status), color = CubePinkAccent, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FaqItem(question: String, answer: String) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = question,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = CubePinkAccent
            )
        }
        if (expanded) {
            Text(
                text = answer,
                modifier = Modifier.padding(top = 8.dp),
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = Color.DarkGray
            )
        }
    }
}
