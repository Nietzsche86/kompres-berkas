package com.pocketcompress

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _config = MutableStateFlow(CompressConfig())
    val config: StateFlow<CompressConfig> = _config.asStateFlow()

    fun updateMode(mode: Mode) {
        _config.value = _config.value.copy(mode = mode)
        _uiState.value = UiState.Idle
    }

    fun updateQuality(quality: Int) {
        _config.value = _config.value.copy(quality = quality)
    }

    fun updateScale(scale: Float) {
        _config.value = _config.value.copy(scale = scale)
    }

    fun updateTargetKb(targetKb: Int?) {
        _config.value = _config.value.copy(targetKb = targetKb)
    }

    fun reset() {
        _uiState.value = UiState.Idle
    }

    fun selectFile(context: Context, uri: Uri, mode: Mode) {
        var name = if (mode == Mode.IMAGE) "image.jpg" else "document.pdf"
        var size = 0L

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1 && !cursor.isNull(nameIndex)) name = cursor.getString(nameIndex)
                if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
            }
        }

        _config.value = _config.value.copy(mode = mode)
        _uiState.value = UiState.Selected(uri = uri, name = name, size = size, mode = mode)
    }

    fun startCompress(context: Context) {
        val selected = _uiState.value as? UiState.Selected ?: return
        viewModelScope.launch {
            _uiState.value = UiState.Running(if (selected.mode == Mode.IMAGE) "Mengompresi foto..." else "Menyiapkan PDF...")
            try {
                val cfg = _config.value
                val resultFile = if (selected.mode == Mode.IMAGE) {
                    ImageEngine.compress(context, selected.uri, cfg)
                } else {
                    PdfEngine.compress(context, selected.uri, cfg) { current, total ->
                        _uiState.value = UiState.Running("Memproses halaman $current dari $total...")
                    }
                }
                val rawName = selected.name.substringBeforeLast('.')
                val outName = if (selected.mode == Mode.IMAGE) "compressed_$rawName.jpg" else "compressed_$rawName.pdf"
                _uiState.value = UiState.Done(origSize = selected.size, tempFile = resultFile, outName = outName)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "Terjadi kesalahan saat kompresi")
            }
        }
    }

    fun saveToUri(context: Context, sourceFile: File, destUri: Uri, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = try {
                context.contentResolver.openOutputStream(destUri)?.use { out ->
                    sourceFile.inputStream().use { input -> input.copyTo(out) }
                }
                true
            } catch (_: Exception) {
                false
            }
            withContext(Dispatchers.Main) {
                onComplete(success)
            }
        }
    }
}
