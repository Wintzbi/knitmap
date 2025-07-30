package com.knitMap

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color

@Composable
fun <T> GenericListWithControls(
    items: List<T>,
    onAdd: () -> Unit,
    onDelete: (index: Int) -> Unit,
    itemContent: @Composable (item: T, index: Int, onDeleteClick: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    itemBackgroundColor: Color = Color(0xFF4E7072) // Couleur de fond par défaut
) {
    Column(modifier = modifier.padding(16.dp)) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            items.forEachIndexed { index, item ->
                // Utiliser Card avec la couleur de fond et les coins arrondis
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp), // Appliquer des coins arrondis ici
                    colors = CardDefaults.cardColors(containerColor = itemBackgroundColor) // Couleur de fond du Card
                ) {
                    itemContent(item, index) {
                        onDelete(index)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onAdd,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4E7072)  // Couleur de fond du bouton "Ajouter"
            )
        ) {
            Text("Ajouter", color = Color.White)
        }
    }
}
