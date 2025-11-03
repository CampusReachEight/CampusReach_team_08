package com.android.sample.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

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

  Scaffold(topBar = { TopAppBar(title = { Text("Edit Profile") }) }) { padding ->
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
          OutlinedTextField(
              value = name,
              onValueChange = { name = it },
              label = { Text("Name") },
          )
          OutlinedTextField(
              value = section, onValueChange = { section = it }, label = { Text("Section") })
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onSave(name, section) }) { Text("Save") }
            OutlinedButton(onClick = onCancel) { Text("Cancel") }
          }
        }
  }
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
