import androidx.compose.ui.window.application


private val gossipAnalyzer = GossipLogAnalyzer()
private val topologyAnalyzer = TopologyAnalyzer()

fun main() = application {
    TopologyAnalyzerWindow(topologyAnalyzer)
    GossipLogAnalyzerWindow(gossipAnalyzer)
}