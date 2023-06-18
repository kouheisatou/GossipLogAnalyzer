import androidx.compose.runtime.*
import java.io.File
import kotlin.system.exitProcess

class TopologyAnalyzer : CSVAnalyzer() {
    override fun analyzeCSVLine(lineText: String?) {
    }

    override fun onAnalyzingFinished() {
    }
}

enum class AnalyzerWindowState {
    Initialized, Analyzing, Analyzed
}
