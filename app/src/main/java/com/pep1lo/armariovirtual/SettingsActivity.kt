package com.pep1lo.armariovirtual

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.pep1lo.armariovirtual.data.BackupData
import com.pep1lo.armariovirtual.ui.ViewModelFactory
import com.pep1lo.armariovirtual.ui.WardrobeViewModel
import com.pep1lo.armariovirtual.ui.theme.ArmarioVirtualTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader

class SettingsActivity : ComponentActivity() {

    private val viewModel: WardrobeViewModel by viewModels {
        ViewModelFactory(
            (application as WardrobeApplication).database.clothingItemDao(),
            (application as WardrobeApplication).database.outfitDao()
        )
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val createDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            lifecycleScope.launch {
                val backupData = viewModel.getBackupData().first()
                val jsonString = json.encodeToString(backupData)
                try {
                    contentResolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(jsonString.toByteArray())
                    }
                    Toast.makeText(this@SettingsActivity, "Backup creado con éxito", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@SettingsActivity, "Error al crear el backup", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val jsonString = contentResolver.openInputStream(it)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).readText()
                }

                if (jsonString != null) {
                    // --- INICIO DE LA MODIFICACIÓN ---
                    // Pre-procesamos el texto del JSON para corregir categorías antiguas
                    // antes de intentar decodificarlo.
                    val cleanedJsonString = jsonString
                        .replace("\"category\": \"Intermedio\"", "\"category\": \"Exterior\"")
                        .replace("\"category\": \"Zapatos\"", "\"category\": \"Exterior\"")
                    // --- FIN DE LA MODIFICACIÓN ---

                    val backupData = json.decodeFromString<BackupData>(cleanedJsonString)
                    viewModel.restoreDataFromBackup(backupData)
                    Toast.makeText(this@SettingsActivity, "Datos restaurados con éxito", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // Añadimos un log para poder ver el error exacto en Logcat si vuelve a fallar
                android.util.Log.e("SettingsActivity", "Error al importar backup", e)
                Toast.makeText(this@SettingsActivity, "Error: Archivo de backup inválido", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ArmarioVirtualTheme {
                SettingsScreen(
                    onExportClick = {
                        createDocumentLauncher.launch("armario_virtual_backup.json")
                    },
                    onImportClick = {
                        openDocumentLauncher.launch(arrayOf("application/json"))
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onExportClick: () -> Unit, onImportClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Ajustes y Copia de Seguridad") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onExportClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Exportar Datos (Backup)")
            }

            Button(
                onClick = onImportClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Importar Datos (Restaurar)")
            }
        }
    }
}

