import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.nio.file.Files
import java.nio.file.Paths

class GossipLogAnalyzer(val logFile: File) {
    var analyzed = mutableStateOf(false)
    var processing = mutableStateOf(false)
    var lineCount = 0
    var maxLine = Files.lines(Paths.get(logFile.path)).count()

    val channels = mutableMapOf<String, Channel>()

    fun analyze(onFinished: () -> Unit, processPerLine: (readingLine: String?, progress: Float) -> Unit) {
        println(maxLine)
        processing.value = true
        CoroutineScope(Dispatchers.Default).launch {
            BufferedReader(FileReader(logFile)).use { br ->
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    val csvElements = line?.split(",") ?: listOf()
                    val channelId = csvElements[2]
                    val channelUpdate = ChannelUpdate(
                        csvElements[0],
                        csvElements[1],
                        channelId,
                        csvElements[3],
                        csvElements[4],
                        csvElements[5],
                        csvElements[6],
                        csvElements[7],
                        csvElements[8],
                        csvElements[9],
                        csvElements[10],
                    )
                    if(channels.contains(channelId)){
                        channels[channelId]?.channelUpdates?.add(channelUpdate)
                    }else{
                        channels[channelId] = Channel(channelId)
                    }

                    processPerLine(line, lineCount.toFloat() / maxLine)
                    lineCount++
                }
                onFinished()
                processing.value = false
                analyzed.value = true
            }
        }
    }
}