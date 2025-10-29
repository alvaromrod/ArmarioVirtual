package com.pep1lo.armariovirtual

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.pep1lo.armariovirtual.data.*
import com.pep1lo.armariovirtual.ui.DropdownMenu
import com.pep1lo.armariovirtual.ui.ViewModelFactory
import com.pep1lo.armariovirtual.ui.WardrobeViewModel
import com.pep1lo.armariovirtual.ui.theme.ArmarioVirtualTheme

class MainActivity : ComponentActivity() {

    private val viewModel: WardrobeViewModel by viewModels {
        ViewModelFactory(
            (application as WardrobeApplication).database.clothingItemDao(),
            (application as WardrobeApplication).database.outfitDao()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ArmarioVirtualTheme {
                val navController = rememberNavController()
                val allItems: List<ClothingItem> by viewModel.allItems.collectAsStateWithLifecycle()
                val savedOutfits: List<OutfitWithItems> by viewModel.savedOutfits.collectAsStateWithLifecycle()
                val generatedOutfit: List<ClothingItem> by viewModel.generatedOutfit.collectAsStateWithLifecycle()
                val wardrobeStats: WardrobeStats by viewModel.wardrobeStats.collectAsStateWithLifecycle()

                MainScreen(
                    navController = navController,
                    viewModel = viewModel,
                    allItems = allItems,
                    allOutfits = savedOutfits,
                    generatedOutfit = generatedOutfit,
                    wardrobeStats = wardrobeStats
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavHostController,
    viewModel: WardrobeViewModel,
    allItems: List<ClothingItem>,
    allOutfits: List<OutfitWithItems>,
    generatedOutfit: List<ClothingItem>,
    wardrobeStats: WardrobeStats
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("El Armario de Sílvia") },
                actions = {
                    IconButton(onClick = { context.startActivity(Intent(context, SettingsActivity::class.java)) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes")
                    }
                }
            )
        },
        bottomBar = { AppBottomNavigation(navController = navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "wardrobe",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("wardrobe") {
                WardrobeScreen(
                    allItems = allItems,
                    onDeleteItem = { viewModel.deleteItem(it) },
                    onToggleAvailability = { viewModel.toggleAvailability(it) },
                    onEditItem = {
                        val intent = Intent(context, AddClothingItemActivity::class.java).apply {
                            putExtra("ITEM_ID", it.id)
                        }
                        context.startActivity(intent)
                    }
                )
            }
            composable("generator") {
                GeneratorScreen(
                    generatedOutfit = generatedOutfit,
                    onGenerate = { season, style, allowMixAndMatch ->
                        viewModel.generateOutfit(season, style, allowMixAndMatch)
                    },
                    onSaveOutfit = {
                        viewModel.saveOutfit(it)
                        // viewModel.clearGeneratedOutfit() // Remove or implement if needed
                    }
                )
            }
            composable("outfits") {
                OutfitsScreen(
                    allOutfits = allOutfits,
                    onMarkAsWorn = { viewModel.markOutfitAsWorn(it) },
                    onDeleteOutfit = { viewModel.deleteOutfit(it) },
                    onEditOutfit = {
                        val intent = Intent(context, EditOutfitActivity::class.java).apply {
                            putExtra("OUTFIT_ID", it.outfit.id)
                        }
                        context.startActivity(intent)
                    }
                )
            }
            composable("stats") {
                StatsScreen(
                    stats = wardrobeStats,
                    onResetStats = { /* viewModel.resetStats() */ } // Commented out, add implementation if needed
                )
            }
        }
    }
}

@Composable
fun WardrobeScreen(
    allItems: List<ClothingItem>,
    onDeleteItem: (ClothingItem) -> Unit,
    onToggleAvailability: (ClothingItem) -> Unit,
    onEditItem: (ClothingItem) -> Unit
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedSeason by remember { mutableStateOf<Season?>(null) }
    var selectedStyle by remember { mutableStateOf<Style?>(null) }

    val filteredItems = allItems.filter {
        (selectedCategory == null || it.category == selectedCategory) &&
                (selectedSeason == null || it.season == selectedSeason) &&
                (selectedStyle == null || it.style == selectedStyle)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { context.startActivity(Intent(context, AddClothingItemActivity::class.java)) }) {
                Icon(Icons.Default.Add, contentDescription = "Añadir prenda")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DropdownMenu(
                    modifier = Modifier.weight(1f),
                    label = "Categoría",
                    options = listOf("Todas") + DataSource.categories.map { it.displayName },
                    selectedOption = selectedCategory?.displayName ?: "Todas",
                    onOptionSelected = { name ->
                        selectedCategory = if (name == "Todas") null else DataSource.categories.find { it.displayName == name }
                    }
                )
                DropdownMenu(
                    modifier = Modifier.weight(1f),
                    label = "Temporada",
                    options = listOf("Todas") + DataSource.seasons.map { it.displayName },
                    selectedOption = selectedSeason?.displayName ?: "Todas",
                    onOptionSelected = { name ->
                        selectedSeason = if (name == "Todas") null else DataSource.seasons.find { it.displayName == name }
                    }
                )
                DropdownMenu(
                    modifier = Modifier.weight(1f),
                    label = "Estilo",
                    options = listOf("Todas") + DataSource.styles.map { it.displayName },
                    selectedOption = selectedStyle?.displayName ?: "Todas",
                    onOptionSelected = { name ->
                        selectedStyle = if (name == "Todas") null else DataSource.styles.find { it.displayName == name }
                    }
                )
            }

            if (filteredItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay prendas que coincidan con los filtros.")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        ClothingCard(
                            item = item,
                            onDelete = { onDeleteItem(item) },
                            onToggleAvailability = { onToggleAvailability(item) },
                            onClick = { onEditItem(item) }
                        )
                    }
                }
            }
        }
    }
}

// REPLACED: Generator screen now uses Scaffold + LazyColumn so content scrolls and the Save button is always visible.
@Composable
fun GeneratorScreen(
    generatedOutfit: List<ClothingItem>,
    onGenerate: (Season, Style, Boolean) -> Unit,
    onSaveOutfit: (List<ClothingItem>) -> Unit
) {
    var selectedSeason by remember { mutableStateOf(DataSource.seasons.first()) }
    var selectedStyle by remember { mutableStateOf(DataSource.styles.first()) }
    var allowMixAndMatch by remember { mutableStateOf(false) }
    var generationAttempted by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            Surface(tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (generatedOutfit.isNotEmpty()) {
                                onSaveOutfit(generatedOutfit)
                                generationAttempted = false
                            }
                        },
                        enabled = generatedOutfit.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Guardar Conjunto")
                    }
                }
            }
        }
    ) { innerPadding ->
        val contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = innerPadding.calculateTopPadding() + 16.dp,
            bottom = innerPadding.calculateBottomPadding() + 16.dp
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                DropdownMenu(
                    label = "Temporada",
                    options = DataSource.seasons.map { it.displayName },
                    selectedOption = selectedSeason.displayName,
                    onOptionSelected = { name ->
                        selectedSeason = DataSource.seasons.first { it.displayName == name }
                    }
                )
            }
            item {
                DropdownMenu(
                    label = "Estilo",
                    options = DataSource.styles.map { it.displayName },
                    selectedOption = selectedStyle.displayName,
                    onOptionSelected = { name ->
                        selectedStyle = DataSource.styles.first { it.displayName == name }
                    }
                )
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = allowMixAndMatch, onCheckedChange = { allowMixAndMatch = it })
                    Spacer(Modifier.width(8.dp))
                    Text("Permitir combinar estilos/temporadas")
                }
            }
            item {
                Button(
                    onClick = {
                        generationAttempted = true
                        onGenerate(selectedSeason, selectedStyle, allowMixAndMatch)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Generar Outfit")
                }
            }

            if (generationAttempted && generatedOutfit.isEmpty()) {
                item {
                    Text(
                        "No se encontraron prendas suficientes para generar un conjunto con esos filtros.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    )
                }
            }

            if (generatedOutfit.isNotEmpty()) {
                item {
                    Text("Conjunto Sugerido", style = MaterialTheme.typography.headlineSmall)
                }
                items(generatedOutfit, key = { it.id }) { item ->
                    Card(elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = item.imageUri,
                                    error = painterResource(id = R.drawable.ic_launcher_background)
                                ),
                                contentDescription = item.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp, max = 260.dp) // prevent single item from overflowing
                                    .clip(MaterialTheme.shapes.medium),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.name,
                                fontSize = 14.sp,
                                maxLines = 2,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                // Keep last card above the bottom bar
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }
}

@Composable
fun OutfitsScreen(
    allOutfits: List<OutfitWithItems>,
    onMarkAsWorn: (OutfitWithItems) -> Unit,
    onDeleteOutfit: (OutfitWithItems) -> Unit,
    onEditOutfit: (OutfitWithItems) -> Unit
) {
    val context = LocalContext.current
    var selectedSeason by remember { mutableStateOf<Season?>(null) }
    var selectedStyle by remember { mutableStateOf<Style?>(null) }

    val filteredOutfits = allOutfits.filter { outfit ->
        val hasSeason = selectedSeason == null || outfit.items.any { it.season == selectedSeason }
        val hasStyle = selectedStyle == null || outfit.items.any { it.style == selectedStyle }
        hasSeason && hasStyle
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { context.startActivity(Intent(context, CreateOutfitActivity::class.java)) }) {
                Icon(Icons.Default.Add, contentDescription = "Crear conjunto")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DropdownMenu(
                    modifier = Modifier.weight(1f),
                    label = "Temporada",
                    options = listOf("Todas") + DataSource.seasons.map { it.displayName },
                    selectedOption = selectedSeason?.displayName ?: "Todas",
                    onOptionSelected = { name ->
                        selectedSeason = if (name == "Todas") null else DataSource.seasons.find { it.displayName == name }
                    }
                )
                DropdownMenu(
                    modifier = Modifier.weight(1f),
                    label = "Estilo",
                    options = listOf("Todas") + DataSource.styles.map { it.displayName },
                    selectedOption = selectedStyle?.displayName ?: "Todas",
                    onOptionSelected = { name ->
                        selectedStyle = if (name == "Todas") null else DataSource.styles.find { it.displayName == name }
                    }
                )
            }
            if (filteredOutfits.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay conjuntos guardados que coincidan.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredOutfits, key = { it.outfit.id }) { outfit ->
                        OutfitCard(
                            outfit = outfit,
                            onMarkAsWorn = { onMarkAsWorn(outfit) },
                            onDelete = { onDeleteOutfit(outfit) },
                            onEdit = { onEditOutfit(outfit) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatsScreen(
    stats: WardrobeStats,
    onResetStats: () -> Unit
) {
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Confirmar Reseteo") },
            text = { Text("¿Estás seguro de que quieres resetear todas las estadísticas de uso? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetStats()
                        showResetDialog = false
                    }
                ) {
                    Text("Resetear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Estadísticas del Armario", style = MaterialTheme.typography.headlineMedium)
            }
        }

        item {
            Card(elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Top 5 Prendas Más Usadas", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    if (stats.top5MostWornItems.isEmpty()) {
                        Text("Aún no has marcado ninguna prenda como usada.")
                    } else {
                        stats.top5MostWornItems.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(item.name)
                                Text("${item.usageCount} usos")
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Distribución por Estilo", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    if (stats.styleDistribution.isEmpty()) {
                        Text("No hay prendas en el armario.")
                    } else {
                        stats.styleDistribution.forEach { (style, count) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(style)
                                Text("$count prendas")
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Distribución por Color", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    if (stats.colorDistribution.isEmpty()) {
                        Text("No hay prendas en el armario.")
                    } else {
                        stats.colorDistribution.forEach { (color, count) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(color)
                                Text("$count prendas")
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = "Resetear Estadísticas")
                Spacer(Modifier.width(8.dp))
                Text("Resetear Estadísticas")
            }
        }
    }
}

@Composable
fun ClothingCard(
    item: ClothingItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onToggleAvailability: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Image(
                painter = rememberAsyncImagePainter(
                    model = item.imageUri,
                    error = painterResource(id = R.drawable.ic_launcher_background)
                ),
                contentDescription = item.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(item.name, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(item.features, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (item.isAvailable) "Disponible" else "No disponible",
                        modifier = Modifier.weight(1f),
                        fontSize = 12.sp
                    )
                    Switch(checked = item.isAvailable, onCheckedChange = { onToggleAvailability() })
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OutfitCard(
    outfit: OutfitWithItems,
    onMarkAsWorn: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Conjunto #${outfit.outfit.id}", fontWeight = FontWeight.Bold)
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (outfit.items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Conjunto vacío")
                    }
                } else {
                    outfit.items.forEach { item ->
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = item.imageUri,
                                error = painterResource(id = R.drawable.ic_launcher_background)
                            ),
                            contentDescription = item.name,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onMarkAsWorn,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Usado ${outfit.outfit.usageCount} veces")
            }
        }
    }
}

@Composable
fun AppBottomNavigation(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Checkroom, contentDescription = "Armario") },
            label = { Text("Armario") },
            selected = currentRoute == "wardrobe",
            onClick = {
                navController.navigate("wardrobe") {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Generador") },
            label = { Text("Generador") },
            selected = currentRoute == "generator",
            onClick = {
                navController.navigate("generator") {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Style, contentDescription = "Conjuntos") },
            label = { Text("Conjuntos") },
            selected = currentRoute == "outfits",
            onClick = {
                navController.navigate("outfits") {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.BarChart, contentDescription = "Estadísticas") },
            label = { Text("Estadísticas") },
            selected = currentRoute == "stats",
            onClick = {
                navController.navigate("stats") {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }
}