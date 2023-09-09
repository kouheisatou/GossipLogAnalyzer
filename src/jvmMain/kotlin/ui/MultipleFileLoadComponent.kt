package ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import java.io.File
import java.io.FilenameFilter

@Composable
fun MultipleFileLoadComponent(files: MutableMap<String, File?>, modifier: Modifier = Modifier) {
    var selectedFile by remember { mutableStateOf<String?>(null) }

    LazyColumn(modifier = modifier) {
        items(files.toList()) {
            Row {
                Text(it.first)
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        selectedFile = it.first
                    }
                ) {
                    Text(it.second?.path.toString())
                }
            }
        }
    }

    if (selectedFile != null) {
        FilePicker(
            filenameFilter = { _, name -> name == selectedFile },
            title = "Choose $selectedFile"
        ) { directory, filename ->
            if (directory == null || filename == null || filename != selectedFile) {
                selectedFile = null
                return@FilePicker
            }

            files[selectedFile!!] = File(directory, filename)
            selectedFile = null
        }
    }
}