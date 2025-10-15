package com.pep1lo.armariovirtual.ui

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.pep1lo.armariovirtual.data.ClothingItem

@Composable
fun SelectableClothingItem(
    item: ClothingItem,
    isSelected: Boolean,
    onItemSelected: (ClothingItem) -> Unit
) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .height(160.dp)
            .border(
                width = 3.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = MaterialTheme.shapes.medium
            )
            .clickable { onItemSelected(item) }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                // CORRECCIÓN: Se comprueba si el String es nulo o vacío de forma segura.
                painter = rememberAsyncImagePainter(model = if (!item.imageUri.isNullOrEmpty()) Uri.parse(item.imageUri) else null),
                contentDescription = item.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentScale = ContentScale.Crop
            )
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun ClothingCategoryRow(
    title: String,
    items: List<ClothingItem>,
    selectedItem: ClothingItem?,
    onItemSelected: (ClothingItem) -> Unit
) {
    if (items.isNotEmpty()) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items) { item ->
                    SelectableClothingItem(
                        item = item,
                        isSelected = item.id == selectedItem?.id,
                        onItemSelected = onItemSelected
                    )
                }
            }
        }
    }
}
