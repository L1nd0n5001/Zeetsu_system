package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.HashAlgorithm
import com.example.ui.CompareMode
import com.example.ui.ZeetsuViewModel
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.RoseDanger
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CompareScreen(
    viewModel: ZeetsuViewModel,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    val mode by viewModel.compareMode.collectAsState()
    val inputA by viewModel.compareInputA.collectAsState()
    val inputB by viewModel.compareInputB.collectAsState()
    val selectedAlgo by viewModel.compareAlgo.collectAsState()
    val matchResult by viewModel.compareMatch.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mode Info
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(DarkSurfaceVariant)
                .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Compare,
                    contentDescription = null,
                    tint = CyanNeon,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Comparador de Hashes & Integridade",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Valide se dois hashes são idênticos ou teste a integridade de um texto contra um checksum.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // Mode switch chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = mode == CompareMode.HASH_VS_HASH,
                onClick = { viewModel.setCompareMode(CompareMode.HASH_VS_HASH) },
                label = { Text("Hash vs Hash", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CyanNeon,
                    selectedLabelColor = Color(0xFF030712),
                    containerColor = DarkSurfaceVariant,
                    labelColor = TextSecondary
                ),
                modifier = Modifier.weight(1f)
            )

            FilterChip(
                selected = mode == CompareMode.TEXT_VS_HASH,
                onClick = { viewModel.setCompareMode(CompareMode.TEXT_VS_HASH) },
                label = { Text("Texto vs Hash (Integridade)", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CyanNeon,
                    selectedLabelColor = Color(0xFF030712),
                    containerColor = DarkSurfaceVariant,
                    labelColor = TextSecondary
                ),
                modifier = Modifier.weight(1f)
            )
        }

        if (mode == CompareMode.TEXT_VS_HASH) {
            // Algorithm choice for Text hashing
            Column {
                Text(
                    text = "Algoritmo de Verificação:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(HashAlgorithm.MD5, HashAlgorithm.SHA_1, HashAlgorithm.SHA_256, HashAlgorithm.SHA_512, HashAlgorithm.CRC32).forEach { algo ->
                        FilterChip(
                            selected = selectedAlgo == algo,
                            onClick = { viewModel.setCompareAlgo(algo) },
                            label = { Text(algo.displayName, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanNeon,
                                selectedLabelColor = Color(0xFF030712),
                                containerColor = DarkSurfaceVariant,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }
            }
        }

        // Input A
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(DarkSurface)
                .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Column {
                Text(
                    text = if (mode == CompareMode.HASH_VS_HASH) "PRIMEIRO HASH (FONTE A)" else "TEXTO ORIGINAL (FONTE A)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanNeon,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = inputA,
                    onValueChange = { viewModel.onCompareInputAChanged(it) },
                    placeholder = { Text("Cole o primeiro hash ou texto...", fontSize = 12.sp, color = TextMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("compare_input_a"),
                    trailingIcon = {
                        IconButton(onClick = {
                            val clip = clipboardManager.getText()?.text
                            if (!clip.isNullOrBlank()) {
                                viewModel.onCompareInputAChanged(clip.trim())
                            }
                        }) {
                            Icon(imageVector = Icons.Default.ContentPaste, contentDescription = "Colar", tint = CyanNeon)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // Input B
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(DarkSurface)
                .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Column {
                Text(
                    text = "SEGUNDO HASH / HASH ESPERADO (FONTE B)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanNeon,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = inputB,
                    onValueChange = { viewModel.onCompareInputBChanged(it) },
                    placeholder = { Text("Cole o segundo hash para verificação...", fontSize = 12.sp, color = TextMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("compare_input_b"),
                    trailingIcon = {
                        IconButton(onClick = {
                            val clip = clipboardManager.getText()?.text
                            if (!clip.isNullOrBlank()) {
                                viewModel.onCompareInputBChanged(clip.trim())
                            }
                        }) {
                            Icon(imageVector = Icons.Default.ContentPaste, contentDescription = "Colar", tint = CyanNeon)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // Result Card
        AnimatedVisibility(visible = matchResult != null) {
            matchResult?.let { matches ->
                val bannerColor = if (matches) EmeraldAccent else RoseDanger
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (matches) Color(0xFF042F2E) else Color(0xFF3F1419))
                        .border(1.dp, bannerColor, RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (matches) Icons.Default.CheckCircle else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (matches) EmeraldLight else RoseDanger,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (matches) "CORRESPONDÊNCIA PERFEITA (100% MATCH)" else "HASHES DIVERGENTES (SEM MATCH)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (matches) EmeraldLight else RoseDanger,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (matches)
                                    "Os valores coincidem bit a bit. Integridade verificada com sucesso!"
                                else
                                    "Os hashes não coincidem. O texto ou arquivo pode ter sido alterado ou corrompido.",
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
