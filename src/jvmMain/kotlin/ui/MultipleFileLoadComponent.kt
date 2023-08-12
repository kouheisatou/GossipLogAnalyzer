package ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import java.io.File

@Composable
fun MultipleFileLoadComponent(files: MutableMap<String, File?>) {
    var selectedFile by remember { mutableStateOf<String?>(null) }

    LazyColumn {
        items(files.toList()) {
            Row {
                Text(it.first)
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
        FileDialog { directory, filename ->
            if (directory == null || filename == null || filename != selectedFile) {
                selectedFile = null
                return@FileDialog
            }

            files[selectedFile!!] = File(directory, filename)
            selectedFile = null
        }
    }
}