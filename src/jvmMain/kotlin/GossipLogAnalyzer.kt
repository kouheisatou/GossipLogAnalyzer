import androidx.compose.runtime.mutableStateOf
import gossip_msg.ChannelUpdate

class GossipLogAnalyzer : CSVAnalyzer() {

    // for displaying channel list on compose window
    var channelsForDisplay = mutableStateOf<List<Channel>?>(null)

    override fun analyzeCSVLine(lineText: String?) {
        if (lineText == null) return
        val csvElements = lineText.split(",")
        val channelUpdate = ChannelUpdate(
            csvElements[0],
            csvElements[1],
            csvElements[2],
            csvElements[3],
            csvElements[4],
            csvElements[5],
            csvElements[6],
            csvElements[7].toFloat(),
            csvElements[8],
            csvElements[9],
            csvElements[10].toFloat(),
        )
        channels.add(channelUpdate)
    }

    fun getChannel(channel: Channel): Channel? {
        return channels.get(channel.shortChannelId)
    }

    override fun onAnalyzingFinished() {
        channelsForDisplay.value = channels.toList()
    }
}