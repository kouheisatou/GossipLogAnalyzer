import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDropEvent
import java.io.File
import kotlin.system.exitProcess

var logFilePath: MutableState<String?> = mutableStateOf(null)
val selectedChannels = mutableStateListOf<Channel>()

@OptIn(ExperimentalComposeUiApi::class)
fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "GossipLogAnalyzer") {
        if (logFilePath.value != null) {
            val analyzer by remember { mutableStateOf(GossipLogAnalyzer(File(logFilePath.value!!))) }
            var progress by remember { mutableStateOf(0f) }
            var readingLine by remember { mutableStateOf("") }

            if (analyzer.analyzed.value) {
                LazyColumn {
                    items(analyzer.channels.toList()) {
                        Row(modifier = Modifier.clickable {
                            if (!selectedChannels.contains(it.second)) {
                                selectedChannels.add(it.second)
                                println(selectedChannels.toList())
                            }
                        }) {
                            Text(it.first)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(it.second.channelUpdates.size.toString())
                        }
                    }
                }
                for (channel in selectedChannels) {
                    key(window){
                        Window(
                            onCloseRequest = { selectedChannels.remove(channel)},
                            title = channel.shortChannelId,
                            onKeyEvent = {
                                if (it.key == Key.Escape) {
                                    selectedChannels.remove(channel)
                                }
                                false
                            },
                        ) {
                            LazyColumn {
                                items(items = channel.channelUpdates) {
                                    Text(it.toString())
                                }
                            }
                        }
                    }
                }
            } else {
                if (analyzer.processing.value) {
                    LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
                    Text(readingLine)
                } else {
                    Button(onClick = {
                        analyzer.analyze(
                            onFinished = {
                                progress = 0f
                                readingLine = ""
                            },
                            processPerLine = { r, p ->
                                progress = p
                                readingLine = r ?: ""
                            }
                        )
                    }) {
                        Text("analyze")
                    }
                }
            }
        } else {
            Text("Drop log file here!", modifier = Modifier.fillMaxSize())

            val target = object : DropTarget() {
                @Synchronized
                override fun drop(evt: DropTargetDropEvent) {
                    try {
                        evt.acceptDrop(DnDConstants.ACTION_REFERENCE)
                        val droppedFiles = evt.transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<*>
                        droppedFiles.first()?.let {
                            logFilePath.value = (it as File).absolutePath
                            println(logFilePath.value!!)
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
            window.contentPane.dropTarget = target
        }
    }
}

@Composable
fun Dropdown(options: List<String>) {
    val expanded = remember { mutableStateOf(false) }
    val selectedOptionText = remember { mutableStateOf(options[0]) }

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
            .size(250.dp, 50.dp)
            .clip(RoundedCornerShape(4.dp))
            .border(BorderStroke(1.dp, Color.LightGray), RoundedCornerShape(4.dp))
            .clickable { expanded.value = !expanded.value },
    ) {
        Text(
            text = selectedOptionText.value,
            modifier = Modifier.padding(start = 10.dp)
        )
        Icon(
            Icons.Filled.ArrowDropDown,
            "contentDescription",
            Modifier.align(Alignment.CenterEnd)
        )
        DropdownMenu(
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false }
        ) {
            options.forEach { selectionOption ->
                DropdownMenuItem(
                    onClick = {
                        selectedOptionText.value = selectionOption
                        expanded.value = false
                    }
                ) {
                    Text(text = selectionOption)
                }
            }
        }
    }
}