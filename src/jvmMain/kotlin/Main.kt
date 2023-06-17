import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDropEvent
import java.io.File

val analyzer = mutableStateOf<GossipLogAnalyzer?>(null)
val errorMsg = mutableStateOf<String?>(null)

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterialApi::class)
fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "GossipLogAnalyzer") {
        var progress by remember { mutableStateOf(0f) }
        var readingLine by remember { mutableStateOf("") }

        if (analyzer.value != null) {
            if (analyzer.value!!.analyzed.value) {

                var selectedChannel by remember { mutableStateOf<Channel?>(null) }
                Column {
                    Row {
                        Text("ChannelID")
                        Spacer(modifier = Modifier.weight(1f))
                        Text("UpdateCount")
                    }
                    Divider(modifier = Modifier.fillMaxWidth())
                    Row {
                        val listState = rememberLazyListState()
                        LazyColumn(modifier = Modifier.weight(1f), state = listState) {
                            items(analyzer.value!!.channels?.sortedByDescending { it.channelUpdates.size }
                                ?: listOf()) {
                                Row(modifier = Modifier.clickable {
                                    selectedChannel = it
                                }) {
                                    Text(it.shortChannelId)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(it.channelUpdates.size.toString())
                                }
                            }
                        }
                        VerticalScrollbar(
                            modifier = Modifier.fillMaxHeight(),
                            adapter = rememberScrollbarAdapter(listState),
                        )
                    }
                }
                if (selectedChannel != null) {

                    Window(
                        onCloseRequest = { selectedChannel = null },
                        title = selectedChannel!!.shortChannelId,
                        onKeyEvent = {
                            if (it.key == Key.Escape) {
                                selectedChannel = null
                            }
                            false
                        },
                    ) {
                        // todo show channel capacity history as a graph
                        LazyColumn {
                            items(items = selectedChannel?.channelUpdates ?: listOf()) {
                                Text(it.toString())
                            }
                        }
                    }
                }
            } else {
                if (analyzer.value!!.processing.value) {
                    LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
                    Text(readingLine)
                } else {
                    Button(onClick = {
                    }) {
                        Text("analyze")
                    }
                }
            }
        } else {
            Text("Drop log file here!", modifier = Modifier.fillMaxSize(), textAlign = TextAlign.Center)

            val target = object : DropTarget() {
                @Synchronized
                override fun drop(evt: DropTargetDropEvent) {
                    try {
                        evt.acceptDrop(DnDConstants.ACTION_REFERENCE)
                        val droppedFiles = evt.transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<*>
                        droppedFiles.first()?.let {
                            val logFile = File((it as File).absolutePath)
                            println(logFile.path)
                            if (!logFile.path.endsWith(".csv")) {
                                errorMsg.value = "File extension is not CSV."
                                return@let
                            }

                            analyzer.value = GossipLogAnalyzer(logFile)
                            analyzer.value!!.analyze(
                                onFinished = {
                                    progress = 0f
                                    readingLine = ""
                                },
                                processPerLine = { r, p ->
                                    progress = p
                                    readingLine = r ?: ""
                                }
                            )
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
            window.contentPane.dropTarget = target
        }

        if (errorMsg.value != null) {
            AlertDialog(
                onDismissRequest = {
                    errorMsg.value = null
                },
                buttons = {
                    TextButton(
                        onClick = {
                            errorMsg.value = null
                        }
                    ) {
                        Text("OK")
                    }
                },
                title = { Text("error") },
                text = { Text(errorMsg.value ?: "") }
            )
        }
    }
}