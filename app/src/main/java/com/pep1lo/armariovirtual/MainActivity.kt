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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.pep1lo.armariovirtual.data.*
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
                title = { Text(stringResource(R.string.app_name)) },
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
        bottomBar = { AppBottomNavigation(navController = navController) },
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
    val items = listOf(
        BottomNavItem.Wardrobe,
        BottomNavItem.Generator,
        BottomNavItem.Outfits,
        BottomNavItem.Stats
    )
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

/* ===================== STATS SCREEN (EXPANDABLE) ===================== */

@Composable
fun StatsScreen(stats: WardrobeStats) {
    var expandMost by remember { mutableStateOf(false) }
    var expandLeast by remember { mutableStateOf(false) }
    var expandColors by remember { mutableStateOf(false) }
    var expandStyles by remember { mutableStateOf(false) }

    if (stats.top5MostWornItems.isEmpty()
        && stats.top5LeastWornItems.isEmpty()
        && stats.colorDistribution.isEmpty()
        && stats.styleDistribution.isEmpty()
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay suficientes datos para mostrar estadísticas.")
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Text("Estadísticas del Armario", style = MaterialTheme.typography.headlineMedium) }

        // Top 5 Más Usadas (expand -> todas desc)
        item {
            StatExpandableCard(
                title = "Top 5 - Más Usadas",
                isExpanded = expandMost,
                onToggle = { expandMost = !expandMost },
                headerContent = {
                    if (stats.top5MostWornItems.isEmpty()) {
                        Text("Aún no hay datos de uso.")
                    } else {
                        stats.top5MostWornItems.forEach { item ->
                            StatRow(item.name, "${item.usageCount} usos")
                        }
                    }
                },
                expandedContent = {
                    if (stats.allItemsByUsageDesc.isEmpty()) {
                        Text("No hay prendas.")
                    } else {
                        stats.allItemsByUsageDesc.forEach { item ->
                            StatRow(item.name, "${item.usageCount} usos")
                        }
                    }
                }
            )
        }

        // Top 5 Menos Usadas (expand -> todas asc)
        item {
            val ascList = stats.allItemsByUsageDesc.reversed()
            StatExpandableCard(
                title = "Top 5 - Menos Usadas",
                isExpanded = expandLeast,
                onToggle = { expandLeast = !expandLeast },
                headerContent = {
                    if (stats.top5LeastWornItems.isEmpty()) {
                        Text("Aún no hay datos de uso.")
                    } else {
                        stats.top5LeastWornItems.forEach { item ->
                            StatRow(item.name, "${item.usageCount} usos")
                        }
                    }
                },
                expandedContent = {
                    if (ascList.isEmpty()) {
                        Text("No hay prendas.")
                    } else {
                        ascList.forEach { item ->
                            StatRow(item.name, "${item.usageCount} usos")
                        }
                    }
                }
            )
        }

        // Colores (expand)
        item {
            val sortedColors = stats.colorDistribution.entries.sortedByDescending { it.value }
            StatExpandableCard(
                title = "Distribución por Color",
                isExpanded = expandColors,
                onToggle = { expandColors = !expandColors },
                headerContent = {
                    if (sortedColors.isEmpty()) {
                        Text("No hay prendas.")
                    } else {
                        sortedColors.take(5).forEach { (color, count) ->
                            StatRow(color, "$count prendas")
                        }
                    }
                },
                expandedContent = {
                    sortedColors.forEach { (color, count) ->
                        StatRow(color, "$count prendas")
                    }
                }
            )
        }

        // Estilos (expand)
        item {
            val sortedStyles = stats.styleDistribution.entries.sortedByDescending { it.value }
            StatExpandableCard(
                title = "Distribución por Estilo",
                isExpanded = expandStyles,
                onToggle = { expandStyles = !expandStyles },
                headerContent = {
                    if (sortedStyles.isEmpty()) {
                        Text("No hay prendas.")
                    } else {
                        sortedStyles.take(5).forEach { (style, count) ->
                            StatRow(style, "$count prendas")
                        }
                    }
                },
                expandedContent = {
                    sortedStyles.forEach { (style, count) ->
                        StatRow(style, "$count prendas")
                    }
                }
            )
        }
    }
}

@Composable
private fun StatExpandableCard(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    headerContent: @Composable ColumnScope.() -> Unit,
    expandedContent: @Composable ColumnScope.() -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Contraer" else "Expandir"
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            headerContent()
            if (isExpanded) {
                Divider(Modifier.padding(vertical = 8.dp))
                expandedContent()
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

/* ===================== OUTFITS SCREEN ===================== */

@Composable
fun OutfitsScreen(
    savedOutfits: List<OutfitWithItems>,
    onMarkAsWorn: (OutfitWithItems) -> Unit,
    onDeleteOutfit: (OutfitWithItems) -> Unit
) {
    val context = LocalContext.current
    var selectedSeason by remember { mutableStateOf<Season?>(null) }
    var selectedStyle by remember { mutableStateOf<Style?>(null) }

    val filteredOutfits = savedOutfits.filter { outfitWithItems ->
        val matchesSeason = selectedSeason == null || outfitWithItems.items.any { it.season == selectedSeason }
        val matchesStyle = selectedStyle == null || outfitWithItems.items.any { it.style == selectedStyle }
        matchesSeason && matchesStyle
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    EnumDropdownMenu(
                        label = "Temporada",
                        options = DataSource.seasons,
                        selectedOption = selectedSeason,
                        onOptionSelected = { selectedSeason = it },
                        optionToString = { it.displayName },
                        allowNull = true
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    EnumDropdownMenu(
                        label = "Estilo",
                        options = DataSource.styles,
                        selectedOption = selectedStyle,
                        onOptionSelected = { selectedStyle = it },
                        optionToString = { it.displayName },
                        allowNull = true
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    selectedSeason = null
                    selectedStyle = null
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Limpiar Filtros") }
        }

        Divider()

        if (filteredOutfits.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text("No hay conjuntos que coincidan con los filtros.", textAlign = TextAlign.Center)
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
                Text("Conjunto #${outfitWithItems.outfit.id}", style = MaterialTheme.typography.titleMedium)
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar conjunto")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar conjunto", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            // FIX: typTypography -> typography
            Text("Usado: ${outfitWithItems.outfit.usageCount} veces", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            outfitWithItems.items.forEach { item ->
                ClothingCard(item = item, onDeleteItem = null, onToggleAvailability = null, onClick = null)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onMarkAsWorn, modifier = Modifier.fillMaxWidth()) {
                Text("Marcar como usado")
            }
        }
    }
}

/* ===================== GENERATOR SCREEN ===================== */

@Composable
fun GeneratorScreen(viewModel: WardrobeViewModel) {
    var selectedSeason by remember { mutableStateOf<Season?>(null) }
    var selectedStyle by remember { mutableStateOf<Style?>(null) }
    val generatedOutfit by viewModel.generatedOutfit.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EnumDropdownMenu(
            label = "Temporada",
            options = DataSource.seasons,
            selectedOption = selectedSeason,
            onOptionSelected = { selectedSeason = it },
            optionToString = { it.displayName }
        )
        Spacer(Modifier.height(8.dp))
        EnumDropdownMenu(
            label = "Estilo",
            options = DataSource.styles,
            selectedOption = selectedStyle,
            onOptionSelected = { selectedStyle = it },
            optionToString = { it.displayName }
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (selectedSeason != null && selectedStyle != null) {
                    viewModel.generateOutfit(selectedSeason!!, selectedStyle!!)
                }
            },
            enabled = selectedSeason != null && selectedStyle != null,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Generar Outfit") }

        Spacer(Modifier.height(24.dp))

        if (generatedOutfit.isNotEmpty()) {
            Text("Tu outfit:", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            generatedOutfit.forEach { item ->
                ClothingCard(item = item, onDeleteItem = null, onToggleAvailability = null, onClick = null)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { viewModel.saveOutfit(generatedOutfit) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Guardar Conjunto") }
        }
    }
}

/* ===================== WARDROBE SCREEN ===================== */

@Composable
fun WardrobeScreenContent(
    items: List<ClothingItem>,
    onDeleteItem: (ClothingItem) -> Unit,
    onToggleAvailability: (ClothingItem) -> Unit
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedSeason by remember { mutableStateOf<Season?>(null) }
    var selectedStyle by remember { mutableStateOf<Style?>(null) }

    val filteredItems = items.filter { item ->
        (selectedCategory == null || item.category == selectedCategory) &&
                (selectedSeason == null || item.season == selectedSeason) &&
                (selectedStyle == null || item.style == selectedStyle)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    EnumDropdownMenu(
                        label = "Categoría",
                        options = DataSource.categories,
                        selectedOption = selectedCategory,
                        onOptionSelected = { selectedCategory = it },
                        optionToString = { it.displayName },
                        allowNull = true
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    EnumDropdownMenu(
                        label = "Temporada",
                        options = DataSource.seasons,
                        selectedOption = selectedSeason,
                        onOptionSelected = { selectedSeason = it },
                        optionToString = { it.displayName },
                        allowNull = true
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            EnumDropdownMenu(
                label = "Estilo",
                options = DataSource.styles,
                selectedOption = selectedStyle,
                onOptionSelected = { selectedStyle = it },
                optionToString = { it.displayName },
                allowNull = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    selectedCategory = null
                    selectedSeason = null
                    selectedStyle = null
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Limpiar Filtros") }
        }

        Divider()

        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay prendas que coincidan con los filtros.", textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredItems, key = { it.id }) { item ->
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

/* ===================== REUSABLE CARD ===================== */

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
        Row(verticalAlignment = Alignment.CenterVertically) {
            // FIX: handle nullable imageUri safely
            val imageModel = item.imageUri?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
            Image(
                painter = rememberAsyncImagePainter(model = imageModel),
                contentDescription = item.name,
                modifier = Modifier.size(100.dp),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(item.features, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Categoría: ${item.category.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Usado: ${item.usageCount} veces",
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
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar prenda", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

/* ===================== GENERIC ENUM DROPDOWN ===================== */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> EnumDropdownMenu(
    label: String,
    options: List<T>,
    selectedOption: T?,
    onOptionSelected: (T?) -> Unit,
    optionToString: (T) -> String,
    enabled: Boolean = true,
    allowNull: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (enabled) expanded = !expanded }) {
        OutlinedTextField(
            value = selectedOption?.let(optionToString) ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            enabled = enabled
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (allowNull) {
                DropdownMenuItem(
                    text = { Text("Todos") },
                    onClick = {
                        onOptionSelected(null)
                        expanded = false
                    }
                )
            }
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionToString(option)) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
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
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (enabled) expanded = !expanded }) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            enabled = enabled
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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