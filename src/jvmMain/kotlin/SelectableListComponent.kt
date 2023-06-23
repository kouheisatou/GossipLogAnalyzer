import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun <T> SelectableListComponent(
    listDataForDisplay: List<T>, // This data is only for displaying list, so it is not always latest. DO NOT refer for getting detail.
    detailWindowTitle: (selectedItem: T?) -> String,
    detailWindowLayout: @Composable FrameWindowScope.(selectedItem: T?) -> Unit,
    listItemLayout: @Composable (listItem: T) -> Unit,
    fetchLatestDetail: (selectedItem: T) -> T?,
    clipboardText: (selectedItem: T) -> String?,
    listTopRowLayout: (@Composable () -> Unit)? = null,
    listTitle: String? = null,
    modifier: Modifier = Modifier,
    findByText: ((searchText: String) -> T?)? = null,
) {
    var selectedItem by remember { mutableStateOf<T?>(null) }
    val focusRequester = remember { FocusRequester() }
    var detailWindowOpened by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    var listComponentSize by remember { mutableStateOf<IntSize?>(null) }
    var listItemComponentSize by remember { mutableStateOf<IntSize?>(null) }

    LaunchedEffect(selectedItem) {
        val index = listDataForDisplay.indexOf(selectedItem)
        if (index !in listDataForDisplay.indices) return@LaunchedEffect

        if (listItemComponentSize != null && listComponentSize != null) {
            val displayingItemsCount = listComponentSize!!.height / listItemComponentSize!!.height
            val startIndex = listState.firstVisibleItemIndex
            val endIndex = startIndex + displayingItemsCount
            if (index !in startIndex..endIndex) {
                listState.scrollToItem(index, scrollOffset = -(listComponentSize?.height ?: 0) / 2)
            }
        } else {
            listState.scrollToItem(index, scrollOffset = 0)
        }
    }

    Column(
        modifier = modifier.onSizeChanged { listComponentSize = it }
    ) {
        if (listTitle != null) {
            Text(listTitle)
        }
        if (findByText != null) {

            var searchText by remember { mutableStateOf("") }
            var result by remember { mutableStateOf<T?>(null) }

            fun search() {
                result = findByText(searchText)
                if (result != null) {
                    println(result.toString())
                    selectedItem = result
                    detailWindowOpened = true
                }
            }

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        modifier = Modifier
                            .onKeyEvent {
                                if (it.type == KeyEventType.KeyDown && it.key.keyCode == Key.Enter.keyCode) {
                                    search()
                                }
                                false
                            },
                        value = searchText,
                        onValueChange = {
                            searchText = it
                        },
                        singleLine = true,
                        isError = (searchText != "" && findByText(searchText) == null),
                    )
                    IconButton(
                        onClick = {
                            search()
                        }
                    ) {
                        Text("🔍")
                    }
                }
            }
        }
        if (listTopRowLayout != null) {
            listTopRowLayout()
        }
        Divider(modifier = Modifier.fillMaxWidth())
        Row {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .focusable(true)
                    .focusRequester(focusRequester),
                state = listState,
            ) {
                items(listDataForDisplay) { listItem ->
                    var mouseHovering by remember { mutableStateOf(false) }
                    var mousePressing by remember { mutableStateOf(false) }
                    Column(
                        modifier = Modifier
                            .onPointerEvent(PointerEventType.Press) {
                                selectedItem = fetchLatestDetail(listItem)
                                focusRequester.requestFocus()
                                detailWindowOpened = true
                                mousePressing = true
                                println(selectedItem)
                            }
                            .onPointerEvent(PointerEventType.Release) {
                                mousePressing = false
                            }
                            .onPointerEvent(PointerEventType.Enter) { mouseHovering = true }
                            .onPointerEvent(PointerEventType.Exit) { mouseHovering = false }
                            .background(
                                if (mousePressing) {
                                    Color.Gray
                                } else if (selectedItem == listItem) {
                                    Color.LightGray
                                } else {
                                    Color.Transparent
                                }
                            )
                            .border(
                                1.dp, if (mouseHovering) {
                                    Color.Gray
                                } else {
                                    Color.Transparent
                                }
                            )
                            .fillMaxWidth()
                            .onSizeChanged { listItemComponentSize = it }
                    ) {
                        listItemLayout(listItem)
                        Divider()
                    }
                }
            }
            VerticalScrollbar(
                modifier = Modifier.fillMaxHeight(),
                adapter = rememberScrollbarAdapter(listState),
            )
        }
    }

    if (selectedItem != null && detailWindowOpened) {

        // detail window
        Window(
            onCloseRequest = { selectedItem = null },
            title = detailWindowTitle(selectedItem),
            onKeyEvent = {
                if (it.type == KeyEventType.KeyDown) {
                    when (it.key.keyCode) {
                        Key.Escape.keyCode -> {
                            detailWindowOpened = false
                        }

                        Key.DirectionDown.keyCode -> {
                            val index = listDataForDisplay.indexOf(selectedItem)
                            if (index < 0) return@Window false
                            if (index + 1 in listDataForDisplay.indices) {
                                val nextEntry = listDataForDisplay[index + 1]
                                selectedItem = fetchLatestDetail(nextEntry)
                                println(selectedItem)
                            }
                        }

                        Key.DirectionUp.keyCode -> {
                            val index = listDataForDisplay.indexOf(selectedItem)
                            if (index < 0) return@Window false
                            if (index - 1 in listDataForDisplay.indices) {
                                val prevEntry = listDataForDisplay[index - 1]
                                selectedItem = fetchLatestDetail(prevEntry)
                                println(selectedItem)
                            }
                        }
                    }
                }
                false
            }
        ) {

            MenuBar {
                Menu("edit") {
                    Item(
                        "Copy ID",
                        onClick = {
                            // copy to clipboard
                            val text = clipboardText(selectedItem ?: return@Item) ?: return@Item
                            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
                            println(text)
                        },
                        shortcut = KeyShortcut(Key.C, meta = true)
                    )
                }
            }

            detailWindowLayout(selectedItem)
        }
    }
}