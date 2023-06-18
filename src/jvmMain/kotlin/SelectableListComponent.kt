import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun <T> SelectableListComponent(
    listData: List<T>, // This data is only for displaying list, so it is not always latest. DO NOT refer for getting detail.
    detailWindowTitle: (selectedItem: T?) -> String,
    detailWindowLayout: @Composable FrameWindowScope.(selectedItem: T?) -> Unit,
    listItemLayout: @Composable (listItem: T) -> Unit,
    fetchLatestDetail: (selectedItem: T) -> T?,
    clipboardText: (selectedItem: T) -> String?,
    listTopRowLayout: (@Composable () -> Unit)? = null,
    listTitle: String? = null,
    modifier: Modifier = Modifier,
    externalControlledSelectedItem: MutableState<T?>? = null,
) {

    var selectedItem by remember { mutableStateOf<T?>(null) }

    fun setSelected(newSelectedItem: T?) {
        if (externalControlledSelectedItem != null) {
            externalControlledSelectedItem.value = newSelectedItem
        } else {
            selectedItem = newSelectedItem
        }
    }

    fun getSelected(): T? {
        return if (externalControlledSelectedItem != null) {
            externalControlledSelectedItem.value
        } else {
            selectedItem
        }
    }

    Column(modifier = modifier) {
        if (listTitle != null) {
            Text(listTitle)
        }
        if (listTopRowLayout != null) {
            listTopRowLayout()
        }
        Divider(modifier = Modifier.fillMaxWidth())
        Row {
            val listState = rememberLazyListState()
            LazyColumn(modifier = Modifier.weight(1f), state = listState) {
                items(listData) {
                    Column(
                        modifier = Modifier
                            .clickable {
                                setSelected(fetchLatestDetail(it))
                            }
                            .background(
                                if (getSelected() == it) {
                                    Color.LightGray
                                } else {
                                    Color.White
                                }
                            )
                            .fillMaxWidth()
                    ) {
                        listItemLayout(it)
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
    if (getSelected() != null) {

        Window(
            onCloseRequest = { setSelected(null) },
            title = detailWindowTitle(getSelected()),
            onKeyEvent = {
                if (it.type == KeyEventType.KeyDown) {
                    when (it.key.keyCode) {
                        Key.Escape.keyCode -> {
                            setSelected(null)
                        }

                        Key.DirectionDown.keyCode -> {
                            val index = listData.indexOf(getSelected()) + 1
                            if (index in listData.indices) {
                                val nextEntry = listData[index]
                                setSelected(fetchLatestDetail(nextEntry))
                            }
                        }

                        Key.DirectionUp.keyCode -> {
                            val index = listData.indexOf(getSelected()) - 1
                            if (index in listData.indices) {
                                setSelected(listData[index])
                                if (index in listData.indices) {
                                    val prevEntry = listData[index]
                                    setSelected(fetchLatestDetail(prevEntry))
                                }
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
                        "Copy",
                        onClick = {
                            // copy to clipboard
                            val text = clipboardText(getSelected() ?: return@Item) ?: return@Item
                            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
                            println(text)
                        },
                        shortcut = KeyShortcut(Key.C, meta = true)
                    )
                }
            }

            detailWindowLayout(getSelected())
        }
    }
}