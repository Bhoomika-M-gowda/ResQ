package com.resq.map

import android.app.Application
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

data class KarnatakaMapState(
    val installed: Boolean = false,
    val importing: Boolean = false,
    val progressPercent: Int = 0,
    val sizeBytes: Long = 0,
    val path: String? = null,
    val error: String? = null
)

class KarnatakaMapPackageManager(private val application: Application) {
    private val mapDirectory = File(application.getExternalFilesDir(null), "maps")
    private val mapFile = File(mapDirectory, "karnataka.pmtiles")
    private val _state = MutableStateFlow(readState())
    val state: StateFlow<KarnatakaMapState> = _state.asStateFlow()

    fun currentFile(): File? = mapFile.takeIf { it.isFile && validHeader(it) }

    suspend fun import(uri: Uri): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            mapDirectory.mkdirs()
            val temporary = File(mapDirectory, "karnataka.importing")
            temporary.delete()
            _state.value = KarnatakaMapState(importing = true)
            val expected = application.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
            application.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "The selected file could not be opened" }
                temporary.outputStream().buffered().use { output ->
                    val buffer = ByteArray(1024 * 1024)
                    var copied = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        copied += count
                        require(copied <= MAX_PACKAGE_BYTES) { "Map is larger than the 1 GB profile" }
                        output.write(buffer, 0, count)
                        val percent = if (expected > 0) ((copied * 100 / expected).coerceIn(0, 99)).toInt() else 0
                        _state.value = KarnatakaMapState(importing = true, progressPercent = percent, sizeBytes = copied)
                    }
                }
            }
            require(validHeader(temporary)) { "This is not a valid PMTiles v3 map" }
            if (mapFile.exists()) mapFile.delete()
            require(temporary.renameTo(mapFile)) { "The imported map could not be saved" }
            _state.value = readState()
            mapFile
        }.onFailure { error ->
            File(mapDirectory, "karnataka.importing").delete()
            _state.value = readState().copy(error = error.message ?: "Map import failed")
        }
    }

    private fun readState(): KarnatakaMapState = currentFile()?.let {
        KarnatakaMapState(installed = true, progressPercent = 100, sizeBytes = it.length(), path = it.absolutePath)
    } ?: KarnatakaMapState()

    private fun validHeader(file: File): Boolean = runCatching {
        if (file.length() < 127) return@runCatching false
        val header = ByteArray(7)
        file.inputStream().use { it.read(header) }
        String(header, Charsets.US_ASCII) == "PMTiles"
    }.getOrDefault(false)

    companion object {
        const val MAX_PACKAGE_BYTES = 1_073_741_824L
    }
}
