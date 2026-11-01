package com.pocketcompress

import android.net.Uri
import java.io.File

enum class Mode { IMAGE, PDF }

data class CompressConfig(
    val mode: Mode = Mode.IMAGE,
    val quality: Int = 70,
    val scale: Float = 0.6f,
    val targetKb: Int? = null
)

sealed interface UiState {
    data object Idle : UiState
    data class Selected(val uri: Uri, val name: String, val size: Long, val mode: Mode) : UiState
    data class Running(val progress: String) : UiState
    data class Done(val origSize: Long, val tempFile: File, val outName: String) : UiState
    data class Error(val msg: String) : UiState
}
