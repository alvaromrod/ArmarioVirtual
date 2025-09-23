package com.pep1lo.armariovirtual

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pep1lo.armariovirtual.data.Category
import com.pep1lo.armariovirtual.data.ClothingItem
import com.pep1lo.armariovirtual.data.OutfitWithItems
import com.pep1lo.armariovirtual.ui.ClothingCategoryRow
import com.pep1lo.armariovirtual.ui.ViewModelFactory
import com.pep1lo.armariovirtual.ui.WardrobeViewModel
import com.pep1lo.armariovirtual.ui.theme.ArmarioVirtualTheme

class EditOutfitActivity : ComponentActivity() {

    private val viewModel: WardrobeViewModel by viewModels {
        ViewModelFactory(
            (application as WardrobeApplication).database.clothingItemDao(),
            (application as WardrobeApplication).database.outfitDao()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val outfitId = intent.getIntExtra("OUTFIT_ID", -1)
        if (outfitId == -1) {
            finish()
            return
        }

        setContent {
            ArmarioVirtualTheme {
                val allItems by viewModel.allItems.collectAsState()
                val currentOutfit by viewModel.currentOutfit.collectAsState()

                LaunchedEffect(outfitId) {
                    viewModel.getOutfitById(outfitId)
                }

                DisposableEffect(Unit) {
                    onDispose {
                        viewModel.clearCurrentOutfit()
                    }
                }

                if (currentOutfit != null) {
                    EditOutfitScreen(
                        allItems = allItems,
                        editingOutfit = currentOutfit,
                        onUpdateOutfit = { selectedItems ->
                            viewModel.updateOutfit(outfitId, selectedItems)
                            finish()
                        }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditOutfitScreen(
    allItems: List<ClothingItem>,
    editingOutfit: OutfitWithItems?,
    onUpdateOutfit: (List<ClothingItem>) -> Unit
) {
    var selectedTop by remember { mutableStateOf<ClothingItem?>(null) }
    var selectedBottom by remember { mutableStateOf<ClothingItem?>(null) }
    var selectedFullBody by remember { mutableStateOf<ClothingItem?>(null) }
    var selectedCoat by remember { mutableStateOf<ClothingItem?>(null) }
    var selectedShoes by remember { mutableStateOf<ClothingItem?>(null) }

    LaunchedEffect(editingOutfit) {
        editingOutfit?.items?.forEach { item ->
            when (item.category) {
                Category.SUPERIOR -> selectedTop = item
                Category.INFERIOR -> selectedBottom = item
                Category.COMPLETO -> selectedFullBody = item
                Category.EXTERIOR -> selectedCoat = item
            }
            if (item.features == "Zapatos") {
                selectedShoes = item
            }
        }
    }

    val groupedItems = allItems.filter { it.isAvailable }.groupBy { it.category }

    val finalSelection = remember(selectedTop, selectedBottom, selectedFullBody, selectedCoat, selectedShoes) {
        listOfNotNull(selectedFullBody ?: selectedTop, selectedBottom, selectedCoat, selectedShoes)
    }

    val isSaveEnabled = (selectedTop != null || selectedFullBody != null) && selectedShoes != null

    Scaffold(
        topBar = { TopAppBar(title = { Text("Editar Conjunto") }) },
        bottomBar = {
            BottomAppBar {
                Button(
                    onClick = { onUpdateOutfit(finalSelection) },
                    enabled = isSaveEnabled,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Guardar Cambios")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardar Cambios")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 80.dp)
        ) {
            ClothingCategoryRow(
                title = "Prendas Completas",
                items = groupedItems[Category.COMPLETO] ?: emptyList(),
                selectedItem = selectedFullBody,
                onItemSelected = { item ->
                    selectedFullBody = if (selectedFullBody?.id == item.id) null else item
                    if (selectedFullBody != null) {
                        selectedTop = null
                        selectedBottom = null
                    }
                }
            )
            ClothingCategoryRow(
                title = "Prendas Superiores",
                items = groupedItems[Category.SUPERIOR] ?: emptyList(),
                selectedItem = selectedTop,
                onItemSelected = { item ->
                    selectedTop = if (selectedTop?.id == item.id) null else item
                    if (selectedTop != null) selectedFullBody = null
                }
            )
            ClothingCategoryRow(
                title = "Prendas Inferiores",
                items = groupedItems[Category.INFERIOR] ?: emptyList(),
                selectedItem = selectedBottom,
                onItemSelected = { item ->
                    selectedBottom = if (selectedBottom?.id == item.id) null else item
                    if (selectedBottom != null) selectedFullBody = null
                }
            )
            ClothingCategoryRow(
                title = "Abrigos y Chaquetas",
                items = groupedItems[Category.EXTERIOR] ?: emptyList(),
                selectedItem = selectedCoat,
                onItemSelected = { item -> selectedCoat = if (selectedCoat?.id == item.id) null else item }
            )
            ClothingCategoryRow(
                title = "Zapatos",
                items = allItems.filter { it.features == "Zapatos" },
                selectedItem = selectedShoes,
                onItemSelected = { item -> selectedShoes = if (selectedShoes?.id == item.id) null else item }
            )
        }
    }
}

