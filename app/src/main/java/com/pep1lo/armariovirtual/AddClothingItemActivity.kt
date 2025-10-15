package com.pep1lo.armariovirtual

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import coil.compose.rememberAsyncImagePainter
import com.pep1lo.armariovirtual.data.*
import com.pep1lo.armariovirtual.ui.DropdownMenu
import com.pep1lo.armariovirtual.ui.ViewModelFactory
import com.pep1lo.armariovirtual.ui.WardrobeViewModel
import com.pep1lo.armariovirtual.ui.theme.ArmarioVirtualTheme
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
// CORRECCIÓN: Se añade la importación de BuildConfig que faltaba.
import com.pep1lo.armariovirtual.BuildConfig

class AddClothingItemActivity : ComponentActivity() {

    private val viewModel: WardrobeViewModel by viewModels {
        ViewModelFactory(
            (application as WardrobeApplication).database.clothingItemDao(),
            (application as WardrobeApplication).database.outfitDao()
        )
    }

    private var tempImageUri: Uri? = null

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            tempImageUri?.let { uri ->
                viewModel.setEditingImageUri(uri.toString())
            }
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            viewModel.setEditingImageUri(it.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val itemId = intent.getIntExtra("ITEM_ID", -1)

        if (itemId != -1) {
            viewModel.getItemById(itemId)
        } else {
            viewModel.clearEditingItem()
        }

        setContent {
            ArmarioVirtualTheme {
                val editingItem by viewModel.editingItem.collectAsState()

                AddClothingItemScreen(
                    editingItem = editingItem,
                    onSaveItem = { item ->
                        lifecycleScope.launch {
                            if (item.id == 0) {
                                viewModel.insertItem(item)
                            } else {
                                viewModel.updateItem(item)
                            }
                            finish()
                        }
                    },
                    onLaunchCamera = {
                        tempImageUri = createImageUri()
                        takePictureLauncher.launch(tempImageUri)
                    },
                    onLaunchGallery = {
                        pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                )
            }
        }
    }

    private fun createImageUri(): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)
        return FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.provider", imageFile)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClothingItemScreen(
    editingItem: ClothingItem?,
    onSaveItem: (ClothingItem) -> Unit,
    onLaunchCamera: () -> Unit,
    onLaunchGallery: () -> Unit
) {
    var name by remember(editingItem) { mutableStateOf(editingItem?.name ?: "") }
    var selectedCategory by remember(editingItem) { mutableStateOf(editingItem?.category) }
    var availableItems by remember(selectedCategory) { mutableStateOf(DataSource.categoryItemMap[selectedCategory] ?: emptyList()) }
    var selectedItemFeature by remember(editingItem) { mutableStateOf(editingItem?.features ?: "") }
    var selectedColor by remember(editingItem) { mutableStateOf(editingItem?.color ?: "") }
    var selectedStyle by remember(editingItem) { mutableStateOf(editingItem?.style) }
    var selectedSeason by remember(editingItem) { mutableStateOf(editingItem?.season) }
    var imageUri by remember(editingItem) { mutableStateOf(editingItem?.imageUri) }
    var showImageDialog by remember { mutableStateOf(false) }

    val isFormValid = name.isNotBlank() && selectedCategory != null && selectedItemFeature.isNotBlank() &&
            selectedColor.isNotBlank() && selectedStyle != null && selectedSeason != null

    LaunchedEffect(editingItem?.imageUri) {
        imageUri = editingItem?.imageUri
    }

    if (showImageDialog) {
        AlertDialog(
            onDismissRequest = { showImageDialog = false },
            title = { Text("Seleccionar imagen") },
            text = { Text("¿Desde dónde quieres añadir la imagen?") },
            confirmButton = {
                TextButton(onClick = {
                    onLaunchCamera()
                    showImageDialog = false
                }) { Text("Cámara") }
            },
            dismissButton = {
                TextButton(onClick = {
                    onLaunchGallery()
                    showImageDialog = false
                }) { Text("Galería") }
            }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(if (editingItem == null) "Añadir Prenda" else "Editar Prenda") }) },
        floatingActionButton = {
            if(isFormValid) {
                FloatingActionButton(
                    onClick = {
                        val newItem = (editingItem ?: ClothingItem()).copy(
                            name = name,
                            category = selectedCategory!!,
                            features = selectedItemFeature,
                            color = selectedColor,
                            style = selectedStyle!!,
                            season = selectedSeason!!,
                            imageUri = imageUri
                        )
                        onSaveItem(newItem)
                    }
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Guardar")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable { showImageDialog = true },
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(model = imageUri),
                        contentDescription = "Prenda",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.AddAPhoto, contentDescription = "Añadir foto", modifier = Modifier.size(48.dp))
                }
            }
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre / Descripción") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            DropdownMenu(
                modifier = Modifier.fillMaxWidth(),
                label = "Categoría",
                options = DataSource.categories.map { it.displayName },
                selectedOption = selectedCategory?.displayName ?: "",
                onOptionSelected = { name ->
                    val category = DataSource.categories.find { it.displayName == name }
                    if (selectedCategory != category) {
                        selectedCategory = category
                        selectedItemFeature = ""
                        availableItems = DataSource.categoryItemMap[category] ?: emptyList()
                    }
                }
            )
            Spacer(Modifier.height(8.dp))
            DropdownMenu(
                modifier = Modifier.fillMaxWidth(),
                label = "Tipo de Prenda",
                options = availableItems,
                selectedOption = selectedItemFeature,
                onOptionSelected = { selectedItemFeature = it },
                enabled = availableItems.isNotEmpty()
            )
            Spacer(Modifier.height(8.dp))
            DropdownMenu(
                modifier = Modifier.fillMaxWidth(),
                label = "Color",
                options = DataSource.allColors.map { it.name },
                selectedOption = selectedColor,
                onOptionSelected = { selectedColor = it }
            )
            Spacer(Modifier.height(8.dp))
            DropdownMenu(
                modifier = Modifier.fillMaxWidth(),
                label = "Estilo",
                options = DataSource.styles.map { it.displayName },
                selectedOption = selectedStyle?.displayName ?: "",
                onOptionSelected = { name -> selectedStyle = DataSource.styles.find { it.displayName == name } }
            )
            Spacer(Modifier.height(8.dp))
            DropdownMenu(
                modifier = Modifier.fillMaxWidth(),
                label = "Temporada",
                options = DataSource.seasons.map { it.displayName },
                selectedOption = selectedSeason?.displayName ?: "",
                onOptionSelected = { name -> selectedSeason = DataSource.seasons.find { it.displayName == name } }
            )
            Spacer(Modifier.height(80.dp))
        }
    }
}

