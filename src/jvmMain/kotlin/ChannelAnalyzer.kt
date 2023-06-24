import androidx.compose.runtime.mutableStateOf
import gossip_msg.ChannelUpdate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class ChannelAnalyzer : CSVAnalyzer() {

    // for displaying channel list on compose window
    var channelsForDisplay = mutableStateOf<List<Channel>?>(null)

    override fun analyzeCSVLine(lineText: String?) {
        if (lineText == null) return
        val csvElements = lineText.split(",")

        val timestamp = try {
            LocalDateTime.parse(
                csvElements[3],
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
            ).toEpochSecond(ZoneOffset.UTC)
        } catch (e: Exception) {
            csvElements[3].toLong()
        }

        val channelUpdate = ChannelUpdate(
            csvElements[0],
            csvElements[1],
            csvElements[2],
            timestamp,
            csvElements[4],
            csvElements[5],
            csvElements[6],
            csvElements[7].toLong(),
            csvElements[8].toLong(),
            csvElements[9].toFloat(),
            csvElements[10].toLong(),
        )
        channels.add(channelUpdate)
    }

    fun getChannel(channel: Channel): Channel? {
        return channels.findChannelById(channel.shortChannelId)
    }

    override fun onAnalyzingFinished() {
        channelsForDisplay.value = channels.toList()
    }
}