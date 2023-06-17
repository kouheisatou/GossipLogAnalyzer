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

    private val channelHashSet = ChannelHashSet()
    var channels: List<Channel>? = null

    fun analyze(onFinished: () -> Unit, processPerLine: (readingLine: String?, progress: Float) -> Unit) {
        processing.value = true
        CoroutineScope(Dispatchers.Default).launch {
            BufferedReader(FileReader(logFile)).use { br ->
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    try {
                        val csvElements = line?.split(",") ?: listOf()
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
                    }catch (e: Exception){
                        errorMsg.value = e.message
                        processing.value = false
                        analyzed.value = false
                        analyzer.value = null
                        return@launch
                    }

                    processPerLine(line, lineCount.toFloat() / maxLine)
                    lineCount++
                }

                channels = channelHashSet.toList()
                onFinished()
                processing.value = false
                analyzed.value = true
            }
        }
    }
}