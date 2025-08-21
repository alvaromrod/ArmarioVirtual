package com.pep1lo.armariovirtual

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import coil.compose.rememberAsyncImagePainter
import com.pep1lo.armariovirtual.data.ClothingItem
import com.pep1lo.armariovirtual.data.DataSource
import com.pep1lo.armariovirtual.ui.ViewModelFactory
import com.pep1lo.armariovirtual.ui.WardrobeViewModel
import com.pep1lo.armariovirtual.ui.theme.ArmarioVirtualTheme
import kotlinx.coroutines.launch
import java.io.File

class AddClothingItemActivity : ComponentActivity() {

    private val viewModel: WardrobeViewModel by viewModels {
        ViewModelFactory(
            (application as WardrobeApplication).database.clothingItemDao(),
            (application as WardrobeApplication).database.outfitDao()
        )
    }

    private var imageUri by mutableStateOf<Uri?>(null)
    private var tempImageUri: Uri? = null

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, flag)
            imageUri = uri
        }
    }

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            val currentTempUri = tempImageUri
            if (currentTempUri != null) {
                imageUri = currentTempUri
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            tempImageUri = getTmpFileUri()
            val currentTempUri = tempImageUri
            if (currentTempUri != null) {
                takePicture.launch(currentTempUri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val itemId = intent.getIntExtra("ITEM_ID", -1)

        setContent {
            ArmarioVirtualTheme {
                val currentItem by viewModel.currentItem.collectAsState()

                LaunchedEffect(itemId) {
                    if (itemId != -1) {
                        viewModel.getItemById(itemId)
                    } else {
                        viewModel.clearCurrentItem()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AddClothingItemScreen(
                        editingItem = currentItem,
                        imageUri = imageUri,
                        onImageSelected = { uri -> imageUri = uri },
                        onItemSave = { itemToSave ->
                            lifecycleScope.launch {
                                if (itemId != -1) {
                                    viewModel.updateItem(itemToSave)
                                } else {
                                    viewModel.insertItem(itemToSave)
                                }
                                finish()
                            }
                        },
                        onLaunchCamera = {
                            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        onLaunchGallery = {
                            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    )
                }
            }
        }
    }

    private fun getTmpFileUri(): Uri {
        val tmpFile = File.createTempFile("tmp_image_file", ".png", cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }
        return FileProvider.getUriForFile(applicationContext, "${applicationContext.packageName}.provider", tmpFile)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClothingItemScreen(
    editingItem: ClothingItem?,
    imageUri: Uri?,
    onImageSelected: (Uri) -> Unit,
    onItemSave: (ClothingItem) -> Unit,
    onLaunchCamera: () -> Unit,
    onLaunchGallery: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var selectedItem by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf("") }
    var selectedSeason by remember { mutableStateOf("") }
    var availableItems by remember { mutableStateOf(listOf<String>()) }
    var showChoiceDialog by remember { mutableStateOf(false) }

    LaunchedEffect(editingItem) {
        if (editingItem != null) {
            name = editingItem.name
            selectedCategory = editingItem.category
            availableItems = DataSource.categoryItemMap[editingItem.category] ?: emptyList()
            selectedItem = editingItem.features
            selectedColor = editingItem.color
            selectedStyle = editingItem.style
            selectedSeason = editingItem.season
            if (editingItem.imageUri.isNotEmpty()) {
                onImageSelected(Uri.parse(editingItem.imageUri))
            }
        }
    }

    val isButtonEnabled = name.isNotBlank() && selectedCategory.isNotBlank() && selectedItem.isNotBlank() &&
            selectedStyle.isNotBlank() && selectedSeason.isNotBlank()

    if (showChoiceDialog) {
        AlertDialog(
            onDismissRequest = { showChoiceDialog = false },
            title = { Text("Seleccionar imagen") },
            text = { Text("¿Desde dónde quieres añadir la imagen?") },
            confirmButton = {
                TextButton(onClick = {
                    showChoiceDialog = false
                    onLaunchCamera()
                }) {
                    Text("Cámara")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showChoiceDialog = false
                    onLaunchGallery()
                }) {
                    Text("Galería")
                }
            }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(if (editingItem != null) "Editar Prenda" else "Añadir Prenda") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .clickable { showChoiceDialog = true },
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(imageUri),
                        contentDescription = "Imagen de la prenda",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("Pulsa para seleccionar una imagen", textAlign = TextAlign.Center)
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre o Descripción") },
                modifier = Modifier.fillMaxWidth()
            )
            DropdownMenu(
                label = "Categoría",
                options = DataSource.categories,
                selectedOption = selectedCategory,
                onOptionSelected = { category ->
                    selectedCategory = category
                    availableItems = DataSource.categoryItemMap[category] ?: emptyList()
                    selectedItem = ""
                }
            )
            DropdownMenu(
                label = "Item",
                options = availableItems,
                selectedOption = selectedItem,
                onOptionSelected = { selectedItem = it },
                enabled = availableItems.isNotEmpty()
            )
            DropdownMenu(
                label = "Color",
                options = DataSource.allColors,
                selectedOption = selectedColor,
                onOptionSelected = { selectedColor = it }
            )
            DropdownMenu(
                label = "Estilo",
                options = DataSource.styles,
                selectedOption = selectedStyle,
                onOptionSelected = { selectedStyle = it }
            )
            DropdownMenu(
                label = "Temporada",
                options = DataSource.seasons,
                selectedOption = selectedSeason,
                onOptionSelected = { selectedSeason = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val itemToSave = ClothingItem(
                        id = editingItem?.id ?: 0,
                        name = name,
                        category = selectedCategory,
                        features = selectedItem,
                        color = selectedColor,
                        style = selectedStyle,
                        season = selectedSeason,
                        imageUri = imageUri?.toString() ?: "",
                        usageCount = editingItem?.usageCount ?: 0,
                        isAvailable = editingItem?.isAvailable ?: true
                    )
                    onItemSave(itemToSave)
                },
                enabled = isButtonEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar Cambios")
            }
        }
    }
}
