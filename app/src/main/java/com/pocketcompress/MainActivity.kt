package com.pocketcompress

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale
import kotlin.math.roundToInt

private val EmeraldPrimary = Color(0xFF059669)
private val EmeraldDark = Color(0xFF064E3B)
private val EmeraldLight = Color(0xFFD1FAE5)
private val EmeraldSurface = Color(0xFFF0FDF4)
private val BackgroundWhite = Color(0xFFF8FAFC)
private val CardWhite = Color(0xFFFFFFFF)
private val TextDark = Color(0xFF0F172A)
private val TextMuted = Color(0xFF475569)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = EmeraldPrimary,
                    onPrimary = Color.White,
                    primaryContainer = EmeraldLight,
                    onPrimaryContainer = EmeraldDark,
                    background = BackgroundWhite,
                    surface = CardWhite,
                    onSurface = TextDark
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = BackgroundWhite) {
                    PocketCompressScreen()
                }
            }
        }
    }
}

@Composable
fun PocketCompressScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val config by viewModel.config.collectAsState()

    val openPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.selectFile(context, it, config.mode) }
    }

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(if (config.mode == Mode.IMAGE) "image/jpeg" else "application/pdf")
    ) { uri ->
        val done = uiState as? UiState.Done
        if (uri != null && done != null) {
            viewModel.saveToUri(context, done.tempFile, uri) { success ->
                Toast.makeText(context, if (success) "File berhasil disimpan" else "Gagal menyimpan file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Surface(shape = RoundedCornerShape(6.dp), color = EmeraldLight) {
                Text(
                    text = "100% OFFLINE UTILITY",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldDark
                )
            }
            Text("PocketCompress", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = TextDark)
            Text("Kompresi foto dan dokumen PDF tanpa koneksi internet.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFE2E8F0).copy(alpha = 0.5f))
                .padding(4.dp)
        ) {
            Mode.entries.forEach { mode ->
                val selected = config.mode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) EmeraldPrimary else Color.Transparent)
                        .clickable { viewModel.updateMode(mode) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (mode == Mode.IMAGE) "Foto" else "Dokumen PDF",
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) Color.White else TextMuted
                    )
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val types = if (config.mode == Mode.IMAGE) arrayOf("image/*") else arrayOf("application/pdf")
                    openPicker.launch(types)
                },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = if (uiState is UiState.Selected) EmeraldSurface else CardWhite),
            border = BorderStroke(1.dp, if (uiState is UiState.Selected) EmeraldPrimary else Color(0xFFCBD5E1))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState is UiState.Selected) {
                    val sel = uiState as UiState.Selected
                    Text("File Terpilih", style = MaterialTheme.typography.labelMedium, color = EmeraldPrimary, fontWeight = FontWeight.SemiBold)
                    Text(sel.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextDark)
                    Text("Ukuran Asli: ${formatSize(sel.size)}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    Text("Ketuk untuk ganti file", style = MaterialTheme.typography.labelSmall, color = EmeraldPrimary)
                } else {
                    Text(
                        text = if (config.mode == Mode.IMAGE) "Pilih Foto untuk Dikompres" else "Pilih Dokumen PDF",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Text(
                        text = if (config.mode == Mode.IMAGE) "JPEG, PNG, WEBP didukung" else "Semua file scan PDF didukung",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Button(
                        onClick = {
                            val types = if (config.mode == Mode.IMAGE) arrayOf("image/*") else arrayOf("application/pdf")
                            openPicker.launch(types)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Buka Dokumen")
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pengaturan Kompresi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextDark)
                    Surface(shape = RoundedCornerShape(8.dp), color = EmeraldLight) {
                        Text(
                            text = "${config.quality}% / ${(config.scale * 100).roundToInt()}%",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldDark
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Kualitas Kompresi", style = MaterialTheme.typography.bodyMedium, color = TextDark)
                        Text("${config.quality}%", fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                    }
                    Slider(
                        value = config.quality.toFloat(),
                        onValueChange = { viewModel.updateQuality(it.roundToInt()) },
                        valueRange = 10f..100f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = EmeraldPrimary,
                            activeTrackColor = EmeraldPrimary,
                            inactiveTrackColor = EmeraldLight
                        )
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Skala Dimensi", style = MaterialTheme.typography.bodyMedium, color = TextDark)
                        Text("${(config.scale * 100).roundToInt()}%", fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                    }
                    Slider(
                        value = config.scale,
                        onValueChange = { viewModel.updateScale((it * 10).roundToInt() / 10f) },
                        valueRange = 0.2f..1.0f,
                        steps = 7,
                        colors = SliderDefaults.colors(
                            thumbColor = EmeraldPrimary,
                            activeTrackColor = EmeraldPrimary,
                            inactiveTrackColor = EmeraldLight
                        )
                    )
                }

                if (config.mode == Mode.IMAGE) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Target Ukuran (Iterasi Otomatis):", style = MaterialTheme.typography.bodyMedium, color = TextDark)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(null to "Bebas", 200 to "200 KB", 500 to "500 KB", 1000 to "1 MB").forEach { (kb, label) ->
                                val isSelected = config.targetKb == kb
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.updateTargetKb(kb) },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = { viewModel.startCompress(context) },
            enabled = uiState is UiState.Selected,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EmeraldPrimary,
                disabledContainerColor = Color(0xFFCBD5E1)
            )
        ) {
            Text(
                text = if (uiState is UiState.Selected) "Mulai Kompresi Sekarang" else "Pilih File Terlebih Dahulu",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        when (val state = uiState) {
            is UiState.Running -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    border = BorderStroke(1.dp, EmeraldLight)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = EmeraldPrimary, trackColor = EmeraldLight)
                        Text(state.progress, style = MaterialTheme.typography.bodyMedium, color = EmeraldDark, fontWeight = FontWeight.Medium)
                    }
                }
            }
            is UiState.Done -> {
                val newSize = state.tempFile.length()
                val savedBytes = (state.origSize - newSize).coerceAtLeast(0)
                val savedPercent = if (state.origSize > 0) (savedBytes * 100 / state.origSize) else 0

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldLight),
                    border = BorderStroke(1.dp, EmeraldPrimary)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Kompresi Berhasil!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EmeraldDark)
                            Surface(shape = RoundedCornerShape(8.dp), color = EmeraldPrimary) {
                                Text(
                                    text = "-$savedPercent%",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Ukuran Awal", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                Text(formatSize(state.origSize), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextDark)
                            }
                            Text("➔", style = MaterialTheme.typography.titleLarge, color = EmeraldPrimary, modifier = Modifier.align(Alignment.CenterVertically))
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Ukuran Akhir", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                Text(formatSize(newSize), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EmeraldDark)
                            }
                        }

                        Button(
                            onClick = { saveLauncher.launch(state.outName) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Text("Simpan ke HP (SAF)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            is UiState.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(state.msg, color = Color(0xFF991B1B), style = MaterialTheme.typography.bodyMedium)
                        OutlinedButton(
                            onClick = { viewModel.reset() },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF991B1B))
                        ) {
                            Text("Coba Lagi")
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes <= 0 -> "0 B"
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format(Locale.US, "%.2f MB", bytes / (1024f * 1024f))
}
