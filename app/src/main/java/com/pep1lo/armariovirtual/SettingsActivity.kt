package com.pep1lo.armariovirtual

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.pep1lo.armariovirtual.data.AppDatabase
import com.pep1lo.armariovirtual.data.BackupData
import com.pep1lo.armariovirtual.data.ClothingItemDao
import com.pep1lo.armariovirtual.data.OutfitDao
import com.pep1lo.armariovirtual.ui.WardrobeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.OutputStream

class SettingsActivity : ComponentActivity() {

    lateinit var viewModel: WardrobeViewModel

    private val createBackupLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            exportBackupToUri(uri)
        } else {
            Toast.makeText(this, "Exportación cancelada", Toast.LENGTH_SHORT).show()
        }
    }

    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            importBackupFromUri(uri)
        } else {
            Toast.makeText(this, "Importación cancelada", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getDatabase(applicationContext)

        viewModel = ViewModelProvider(
            this,
            WardrobeViewModelFactory(db.clothingItemDao(), db.outfitDao())
        )[WardrobeViewModel::class.java]

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SettingsScreen(
                        onExportClick = { startExport() },
                        onImportClick = { startImport() }
                    )
                }
            }
        }
    }

    private fun openOutputStreamCompat(uri: Uri): OutputStream? {
        return contentResolver.openOutputStream(uri)
            ?: contentResolver.openOutputStream(uri, "rwt")
            ?: contentResolver.openOutputStream(uri, "w")
    }

    private fun exportBackupToUri(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val backupData: BackupData = viewModel.getBackupDataOnce()
                val json = Json {
                    prettyPrint = true
                    encodeDefaults = true
                    explicitNulls = false
                }.encodeToString(BackupData.serializer(), backupData)

                val out = openOutputStreamCompat(uri) ?: run {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@SettingsActivity,
                            "No se pudo abrir el archivo para escribir",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }

                out.use { os ->
                    val bytes = json.toByteArray(Charsets.UTF_8)
                    os.write(bytes)
                    os.flush()
                }

                val writtenBytes = contentResolver.openInputStream(uri)?.use { ins ->
                    ins.readBytes().size
                } ?: 0

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@SettingsActivity,
                        "Backup exportado ($writtenBytes bytes)",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("SettingsActivity", "Error exportando backup", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@SettingsActivity,
                        "Error al exportar backup: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun importBackupFromUri(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val jsonText = contentResolver.openInputStream(uri)?.use { input ->
                    input.readBytes().toString(Charsets.UTF_8)
                } ?: ""

                val backupData = Json {
                    ignoreUnknownKeys = true
                }.decodeFromString(BackupData.serializer(), jsonText)

                viewModel.restoreDataFromBackupOnce(backupData)

                val after = viewModel.getBackupDataOnce()

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@SettingsActivity,
                        "Backup importado (${after.clothingItems.size} prendas)",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            } catch (e: Exception) {
                Log.e("SettingsActivity", "Error importando backup", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@SettingsActivity,
                        "Error al importar backup: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    fun startExport() {
        createBackupLauncher.launch("armario_virtual_backup.json")
    }

    fun startImport() {
        openDocumentLauncher.launch(arrayOf("application/json"))
    }
}

class WardrobeViewModelFactory(
    private val clothingItemDao: ClothingItemDao,
    private val outfitDao: OutfitDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WardrobeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WardrobeViewModel(clothingItemDao, outfitDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
fun SettingsScreen(
    onExportClick: () -> Unit,
    onImportClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = onExportClick, modifier = Modifier.fillMaxWidth()) {
                Text("Exportar backup")
            }
            Spacer(modifier = Modifier.padding(8.dp))
            Button(onClick = onImportClick, modifier = Modifier.fillMaxWidth()) {
                Text("Importar backup")
            }
        }
    }
}