import androidx.compose.runtime.*
import gossip_msg.ChannelAnnouncement
import java.io.File

class NodeAnalyzer : CSVAnalyzer() {

    var nodeListForDisplay = mutableStateOf<List<Node>?>(null)
    val selectedNode = mutableStateOf<Node?>(null)

    override fun analyzeCSVLine(lineText: String?) {
        if (lineText == null) return
        val csvElement = lineText.split(",")
        val channelAnnouncement = ChannelAnnouncement(
            csvElement[0],
            csvElement[1],
            csvElement[2],
            csvElement[3],
            csvElement[4],
            csvElement[5],
            csvElement[6],
            csvElement[7],
            csvElement[8],
            csvElement[9],
            csvElement[10],
        )

        val node1 = Node(channelAnnouncement.nodeId1)
        val node2 = Node(channelAnnouncement.nodeId2)

        val channel = channels.findChannelById(channelAnnouncement.shortChannelId)?.apply {
            this.node1 = node1
            this.node2 = node2
        } ?: return

        nodes.add(node1, channel)
        nodes.add(node2, channel)
    }

    override fun onAnalyzingFinished() {
        nodeListForDisplay.value = nodes.toList()
    }

    override fun onLogFileLoaded(logFile: File): String? {
        return if (channelAnalyzer.state.value != AnalyzerWindowState.Analyzed) {
            "Analyze channel_update log first."
        } else {
            null
        }
    }
}