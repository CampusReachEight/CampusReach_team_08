package com.android.sample.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.sample.ui.theme.AppPalette
import com.android.sample.ui.theme.appPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    initialName: String,
    initialSection: String,
    onSave: (String, String) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var section by remember { mutableStateOf(initialSection) }
    val palette = appPalette()

    Scaffold(topBar = { TopAppBar(title = { Text("Edit Profile") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProfileOutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = "Name",
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                palette = palette
            )

            ProfileOutlinedTextField(
                value = section,
                onValueChange = { section = it },
                label = "Section",
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                palette = palette
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onSave(name, section) }) { Text("Save") }
                OutlinedButton(onClick = onCancel) { Text("Cancel") }
            }
        }
    }
}

@Composable
fun ProfileOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    palette: AppPalette = appPalette()
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = palette.text) },
        singleLine = singleLine,
        modifier = modifier,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = palette.text,
            unfocusedTextColor = palette.text,
            cursorColor = palette.accent,
            focusedBorderColor = palette.accent,
            unfocusedBorderColor = palette.primary.copy(alpha = 0.6f),
            focusedContainerColor = palette.surface,
            unfocusedContainerColor = palette.surface
        )
    )
}

@Preview(showBackground = true)
@Composable
fun EditProfileScreenPreview() {
    EditProfileScreen(
        initialName = "John Doe",
        initialSection = "Computer Science",
        onSave = { _, _ -> },
        onCancel = {})
}