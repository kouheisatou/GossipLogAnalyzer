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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun <T> SelectableListComponent(
    listData: List<T>,
    detailWindowTitle: (selectedItem: T?) -> String,
    detailWindowLayout: @Composable FrameWindowScope.(selectedItem: T?) -> Unit,
    listItemLayout: @Composable RowScope.(listItem: T) -> Unit,
    listTopRowLayout: (@Composable () -> Unit)? = null,
    listTitle: String? = null,
    modifier: Modifier = Modifier,
    externalControlledSelectedItem: MutableState<T?>? = null,
    onItemSelected: ((selectedItem: T) -> Unit)? = null
) {
    val selectedItem = externalControlledSelectedItem ?: mutableStateOf<T?>(null)

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
                    Column {
                        Row(
                            modifier = Modifier
                                .clickable {
                                    selectedItem.value = it
                                    if (onItemSelected != null) {
                                        onItemSelected(it)
                                    }
                                }
                                .background(
                                    if (selectedItem.value == it) {
                                        Color.LightGray
                                    } else {
                                        Color.White
                                    }
                                )
                                .fillMaxWidth()
                        ) {
                            listItemLayout(it)
                        }
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
    if (selectedItem.value != null) {

        Window(
            onCloseRequest = { selectedItem.value = null },
            title = detailWindowTitle(selectedItem.value),
            onKeyEvent = {
                if (it.type == KeyEventType.KeyDown) {
                    when (it.key.keyCode) {
                        Key.Escape.keyCode -> {
                            selectedItem.value = null
                        }

                        Key.DirectionDown.keyCode -> {
                            val index = listData.indexOf(selectedItem.value) + 1
                            if (index in listData.indices) {
                                selectedItem.value = listData[index]
                                if (onItemSelected != null) {
                                    onItemSelected(selectedItem.value!!)
                                }
                            }
                        }

                        Key.DirectionUp.keyCode -> {
                            val index = listData.indexOf(selectedItem.value) - 1
                            if (index in listData.indices) {
                                selectedItem.value = listData[index]
                                if (onItemSelected != null) {
                                    onItemSelected(selectedItem.value!!)
                                }
                            }
                        }
                    }
                }
                false
            }
        ) {
            detailWindowLayout(selectedItem.value)
        }
    }
}