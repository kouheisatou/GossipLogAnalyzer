import androidx.compose.runtime.*
import gossip_msg.ChannelAnnouncement
import java.io.File

class NodeAnalyzer : CSVAnalyzer() {

    var nodeListForDisplay = mutableStateOf<List<Node>?>(null)

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

        val channel = channels.findChannelById(channelAnnouncement.shortChannelId)

        nodes.add(channelAnnouncement.nodeId1, channelAnnouncement.nodeId2, channel)
    }

    override fun onAnalyzingFinished() {
        nodeListForDisplay.value = nodes.toList()
    }

    override fun onLogFileLoaded(logFile: File): String? {
        return if (channelAnalyzer.state.value != AnalyzerWindowState.Analyzed) {
            "Analyze channel_update log first."
        } else if (!logFile.name.startsWith("channel_announcement")) {
            "This file is not a channel_announcement log."
        } else {
            null
        }
    }

    override fun reset() {
        nodes.reset()
        nodeListForDisplay.value = null
    }
}