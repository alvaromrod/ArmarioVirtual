package com.pep1lo.armariovirtual

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import coil.compose.rememberAsyncImagePainter
import com.pep1lo.armariovirtual.data.ClothingItem
import com.pep1lo.armariovirtual.data.DataSource
import com.pep1lo.armariovirtual.data.OutfitWithItems
import com.pep1lo.armariovirtual.data.WardrobeStats
import com.pep1lo.armariovirtual.ui.ViewModelFactory
import com.pep1lo.armariovirtual.ui.WardrobeViewModel
import com.pep1lo.armariovirtual.ui.theme.ArmarioVirtualTheme

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Wardrobe : BottomNavItem("wardrobe", Icons.Default.Checkroom, "Armario")
    object Generator : BottomNavItem("generator", Icons.Default.AutoAwesome, "Generador")
    object Outfits : BottomNavItem("outfits", Icons.Default.Style, "Conjuntos")
    object Stats : BottomNavItem("stats", Icons.Default.Analytics, "Estadísticas")
}

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
                MainScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: WardrobeViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) }, // <-- CAMBIO REALIZADO AQUÍ
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                actions = {
                    IconButton(onClick = {
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes")
                    }
                }
            )
        },
        bottomBar = {
            AppBottomNavigation(navController = navController)
        },
        floatingActionButton = {
            when (currentRoute) {
                BottomNavItem.Wardrobe.route -> {
                    FloatingActionButton(onClick = {
                        context.startActivity(Intent(context, AddClothingItemActivity::class.java))
                    }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Añadir prenda")
                    }
                }
                BottomNavItem.Outfits.route -> {
                    FloatingActionButton(onClick = {
                        context.startActivity(Intent(context, CreateOutfitActivity::class.java))
                    }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Crear conjunto")
                    }
                }
            }
        }
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            viewModel = viewModel,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun AppBottomNavigation(navController: NavHostController) {
    val items = listOf(BottomNavItem.Wardrobe, BottomNavItem.Generator, BottomNavItem.Outfits, BottomNavItem.Stats)
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
fun AppNavHost(navController: NavHostController, viewModel: WardrobeViewModel, modifier: Modifier) {
    NavHost(navController, startDestination = BottomNavItem.Wardrobe.route, modifier = modifier) {
        composable(BottomNavItem.Wardrobe.route) {
            val items by viewModel.allItems.collectAsState()
            WardrobeScreenContent(
                items = items,
                onDeleteItem = viewModel::deleteItem,
                onToggleAvailability = viewModel::toggleAvailability
            )
        }
        composable(BottomNavItem.Generator.route) {
            GeneratorScreen(viewModel = viewModel)
        }
        composable(BottomNavItem.Outfits.route) {
            val savedOutfits by viewModel.savedOutfits.collectAsState()
            OutfitsScreen(
                savedOutfits = savedOutfits,
                onMarkAsWorn = viewModel::markOutfitAsWorn,
                onDeleteOutfit = viewModel::deleteOutfit
            )
        }
        composable(BottomNavItem.Stats.route) {
            val stats by viewModel.wardrobeStats.collectAsState()
            StatsScreen(stats = stats)
        }
    }
}

@Composable
fun StatsScreen(stats: WardrobeStats) {
    if (stats.top5MostWornItems.isEmpty() && stats.colorDistribution.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay suficientes datos para mostrar estadísticas.")
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (stats.top5MostWornItems.isNotEmpty()) {
            Text("Tus 5 prendas favoritas", style = MaterialTheme.typography.titleLarge)
            stats.top5MostWornItems.forEach { item ->
                ClothingCard(item = item, onDeleteItem = null, onToggleAvailability = null, onClick = null)
            }
        }

        if (stats.colorDistribution.isNotEmpty()) {
            Text("Tu paleta de colores", style = MaterialTheme.typography.titleLarge)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    stats.colorDistribution.forEach { (color, count) ->
                        Text("$color: $count prendas")
                    }
                }
            }
        }

        if (stats.styleDistribution.isNotEmpty()) {
            Text("Tus estilos", style = MaterialTheme.typography.titleLarge)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    stats.styleDistribution.forEach { (style, count) ->
                        Text("$style: $count prendas")
                    }
                }
            }
        }
    }
}


@Composable
fun OutfitsScreen(
    savedOutfits: List<OutfitWithItems>,
    onMarkAsWorn: (OutfitWithItems) -> Unit,
    onDeleteOutfit: (OutfitWithItems) -> Unit
) {
    val context = LocalContext.current

    var selectedSeason by remember { mutableStateOf("Todas") }
    var selectedStyle by remember { mutableStateOf("Todos") }

    // Filtramos los outfits basándonos en si alguna de sus prendas coincide con los filtros
    val filteredOutfits = savedOutfits.filter { outfitWithItems ->
        val matchesSeason = selectedSeason == "Todas" || outfitWithItems.items.any { it.season == selectedSeason }
        val matchesStyle = selectedStyle == "Todos" || outfitWithItems.items.any { it.style == selectedStyle }
        matchesSeason && matchesStyle
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // --- INICIO DE LA UI DE FILTROS PARA OUTFITS ---
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    DropdownMenu(
                        label = "Temporada",
                        options = listOf("Todas") + DataSource.seasons,
                        selectedOption = selectedSeason,
                        onOptionSelected = { selectedSeason = it }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    DropdownMenu(
                        label = "Estilo",
                        options = listOf("Todos") + DataSource.styles,
                        selectedOption = selectedStyle,
                        onOptionSelected = { selectedStyle = it }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                selectedSeason = "Todas"
                selectedStyle = "Todos"
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Limpiar Filtros")
            }
        }
        // --- FIN DE LA UI DE FILTROS ---

        Divider()

        if (filteredOutfits.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text(text = "No hay conjuntos que coincidan con los filtros.", textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredOutfits) { outfitWithItems ->
                    OutfitCard(
                        outfitWithItems = outfitWithItems,
                        onMarkAsWorn = { onMarkAsWorn(outfitWithItems) },
                        onEdit = {
                            val intent = Intent(context, EditOutfitActivity::class.java).apply {
                                putExtra("OUTFIT_ID", outfitWithItems.outfit.id)
                            }
                            context.startActivity(intent)
                        },
                        onDelete = { onDeleteOutfit(outfitWithItems) }
                    )
                }
            }
        }
    }
}

@Composable
fun OutfitCard(
    outfitWithItems: OutfitWithItems,
    onMarkAsWorn: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Conjunto #${outfitWithItems.outfit.id}",
                    style = MaterialTheme.typography.titleMedium
                )
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar conjunto")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar conjunto", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Text(
                text = "Usado: ${outfitWithItems.outfit.usageCount} veces",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            outfitWithItems.items.forEach { item ->
                ClothingCard(item = item, onDeleteItem = null, onToggleAvailability = null, onClick = null)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onMarkAsWorn,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Marcar como usado")
            }
        }
    }
}

@Composable
fun GeneratorScreen(viewModel: WardrobeViewModel) {
    var selectedSeason by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf("") }
    val generatedOutfit by viewModel.generatedOutfit.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DropdownMenu(
            label = "Temporada",
            options = DataSource.seasons,
            selectedOption = selectedSeason,
            onOptionSelected = { selectedSeason = it }
        )
        Spacer(modifier = Modifier.height(8.dp))
        DropdownMenu(
            label = "Estilo",
            options = DataSource.styles,
            selectedOption = selectedStyle,
            onOptionSelected = { selectedStyle = it }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { viewModel.generateOutfit(selectedSeason, selectedStyle) },
            enabled = selectedSeason.isNotBlank() && selectedStyle.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Generar Outfit")
        }
        Spacer(modifier = Modifier.height(24.dp))
        if (generatedOutfit.isNotEmpty()) {
            Text("Tu outfit:", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            generatedOutfit.forEach { item ->
                ClothingCard(item = item, onDeleteItem = null, onToggleAvailability = null, onClick = null)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.saveOutfit(generatedOutfit) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar Conjunto")
            }
        }
    }
}

@Composable
fun WardrobeScreenContent(
    items: List<ClothingItem>,
    onDeleteItem: (ClothingItem) -> Unit,
    onToggleAvailability: (ClothingItem) -> Unit
) {
    val context = LocalContext.current

    var selectedCategory by remember { mutableStateOf("Todas") }
    var selectedSeason by remember { mutableStateOf("Todas") }
    var selectedStyle by remember { mutableStateOf("Todos") }

    val filteredItems = items.filter { item ->
        (selectedCategory == "Todas" || item.category == selectedCategory) &&
                (selectedSeason == "Todas" || item.season == selectedSeason) &&
                (selectedStyle == "Todos" || item.style == selectedStyle)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    DropdownMenu(
                        label = "Categoría",
                        options = listOf("Todas") + DataSource.categories,
                        selectedOption = selectedCategory,
                        onOptionSelected = { selectedCategory = it }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    DropdownMenu(
                        label = "Temporada",
                        options = listOf("Todas") + DataSource.seasons,
                        selectedOption = selectedSeason,
                        onOptionSelected = { selectedSeason = it }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            DropdownMenu(
                label = "Estilo",
                options = listOf("Todos") + DataSource.styles,
                selectedOption = selectedStyle,
                onOptionSelected = { selectedStyle = it }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                selectedCategory = "Todas"
                selectedSeason = "Todas"
                selectedStyle = "Todos"
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Limpiar Filtros")
            }
        }

        Divider()

        if (filteredItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text(text = "No hay prendas que coincidan con los filtros.", textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items = filteredItems, key = { item -> item.id }) { item ->
                    ClothingCard(
                        item = item,
                        onDeleteItem = { onDeleteItem(item) },
                        onToggleAvailability = { onToggleAvailability(item) },
                        onClick = {
                            val intent = Intent(context, AddClothingItemActivity::class.java).apply {
                                putExtra("ITEM_ID", item.id)
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ClothingCard(
    item: ClothingItem,
    onDeleteItem: (() -> Unit)?,
    onToggleAvailability: ((ClothingItem) -> Unit)?,
    onClick: (() -> Unit)?
) {
    val alpha = if (item.isAvailable) 1f else 0.5f

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .alpha(alpha)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(model = if (item.imageUri.isNotEmpty()) Uri.parse(item.imageUri) else null),
                contentDescription = item.name,
                modifier = Modifier.size(100.dp),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.features,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Categoría: ${item.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Usado: ${item.usageCount} veces",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onToggleAvailability != null) {
                Switch(
                    checked = item.isAvailable,
                    onCheckedChange = { onToggleAvailability(item) }
                )
            }
            if (onDeleteItem != null) {
                IconButton(onClick = onDeleteItem) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar prenda",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenu(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            enabled = enabled
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
