package analyzer

import network.Channel
import androidx.compose.runtime.mutableStateOf
import channelAnnouncementAnalyzer
import model.input_gossip_msg.ChannelUpdate
import network.Network
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class ChannelUpdateAnalyzer(private val estimatedNetwork: Network) : CSVAnalyzer() {

    // for displaying channel list on compose window
    var channelsForDisplay = mutableStateOf<List<Channel>?>(null)

    override fun analyzeCSVLine(csvElements: List<String>) {
        val timestamp = try {
            LocalDateTime.parse(
                csvElements[3],
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
            ).toEpochSecond(ZoneOffset.UTC)
        } catch (e: Exception) {
            csvElements[3].toLong()
        }

        val id = csvElements[2]

        val channelUpdate = ChannelUpdate(
            csvElements[0],
            csvElements[1],
            id,
            timestamp,
            csvElements[4],
            csvElements[5],
            csvElements[6],
            csvElements[7].toLong(),
            csvElements[8].toLong(),
            csvElements[9].toLong(),
            csvElements[10].toLong(),
        )

        val channel = estimatedNetwork.channels[channelUpdate.shortChannelId]
        channel?.channelUpdates?.add(channelUpdate)
    }

    override fun onAnalyzingFinished() {
        val list = mutableListOf<Channel>()
        estimatedNetwork.channels.toList().sortedByDescending { it.second.channelUpdates.size }.forEach {
            list.add(it.second)
        }
        channelsForDisplay.value = list
    }

    override fun onLogFileLoaded(logFile: File): String? {
        return if (channelAnnouncementAnalyzer.state.value != AnalyzerWindowState.Analyzed) {
            "Analyze channel_announcement log first."
        } else if (!logFile.name.startsWith("channel_update")) {
            "This file is not a channel_update log."
        } else {
            null
        }
    }
}