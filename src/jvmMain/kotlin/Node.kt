import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

class Node(val id: String) {
    val channels = mutableListOf<Channel>()

    override fun equals(other: Any?): Boolean {
        return other is Node && id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Node(id='$id', channels=$channels)"
    }
}

@Composable
fun NodeDetailComponent(node: Node) {
    SelectableListComponent(
        node.channels,
        detailWindowTitle = { "Channel ${it?.shortChannelId}" },
        detailWindowLayout = {
            if (it != null) {
                ChannelDetailComponent(it)
            }
        },
        listItemLayout = {
            Column{
                Text(it.shortChannelId)
                Row{
                    Text(it.node1?.id.toString())
                    Text(" - ", modifier = Modifier.weight(1f))
                    Text(it.node2?.id.toString())
                }
            }
        },
        listTitle = "Channels",
        onItemSelected = {
            println(it)
        }
    )
}