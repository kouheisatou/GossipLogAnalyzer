import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.plot.XYPlot
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer
import org.jfree.data.xy.XYDataItem
import org.jfree.data.xy.XYSeries
import org.jfree.data.xy.XYSeriesCollection
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess

class GossipLogAnalyzer : CSVAnalyzer() {

    private val channelHashSet = ChannelHashSet()

    // for displaying channel list on compose window
    var channels: List<Channel>? = null
    override fun analyzeCSVLine(lineText: String?) {
        val csvElements = lineText?.split(",") ?: listOf()
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
        channelHashSet.add(channelUpdate)
    }

    fun getChannel(channel: Channel): Channel?{
        return channelHashSet.get(channel)
    }

    override fun onAnalyzingFinished() {
        channels = channelHashSet.toList()
    }
}