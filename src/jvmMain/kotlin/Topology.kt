import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.window.Window
import com.google.common.graph.MutableNetwork
import com.google.common.graph.NetworkBuilder
import edu.uci.ics.jung.graph.util.Graphs
import edu.uci.ics.jung.layout.algorithms.BalloonLayoutAlgorithm
import edu.uci.ics.jung.layout.algorithms.FRLayoutAlgorithm
import edu.uci.ics.jung.layout.algorithms.StaticLayoutAlgorithm
import edu.uci.ics.jung.visualization.BaseVisualizationModel
import edu.uci.ics.jung.visualization.VisualizationViewer
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse
import edu.uci.ics.jung.visualization.control.ModalGraphMouse
import java.awt.BasicStroke
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.KeyStroke


class Topology {
    val g: MutableNetwork<Node, Channel> = Graphs.synchronizedNetwork(
        NetworkBuilder
            .directed()
            .allowsParallelEdges(true)
            .allowsSelfLoops(true)
            .build()
    )

    var maxBaseFee = 0
    var maxChannelUpdateCount = 0

    init {
        for (channel in channels.toList()) {
            if (channel.node2 != null && channel.node1 != null) {
                g.addEdge(channel.node1!!, channel.node2!!, channel)

                if (channel.channelUpdates.size > maxChannelUpdateCount) {
                    maxChannelUpdateCount = channel.channelUpdates.size
                }
                for (channelUpdate in channel.channelUpdates) {
                    if (channelUpdate.baseFee > maxBaseFee) {
                        maxBaseFee = channelUpdate.baseFee
                    }
                }
            }
        }
    }
}

@Composable
fun TopologyWindow(topology: Topology) {

    Window(
        onCloseRequest = {},
        title = "topology",
    ) {
        var selectedNode by remember { mutableStateOf<Node?>(null) }
        var selectedChannel by remember { mutableStateOf<Channel?>(null) }
        SwingPanel(
            modifier = Modifier.fillMaxSize(),
            factory = {
                val graphSize = Dimension(19200, 10800)
                val viewer = VisualizationViewer(
                    BaseVisualizationModel(
                        topology.g,
                        StaticLayoutAlgorithm(),
                        graphSize
                    ),
                    graphSize,
                )

                viewer.graphMouse = DefaultModalGraphMouse<Node, Channel>().apply {
                    setMode(ModalGraphMouse.Mode.TRANSFORMING)
                }

                viewer.setNodeToolTipFunction {
                    if (it != null) {
                        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(it.id), null)
                    }
                    it?.toString()
                }

                viewer.renderContext.setEdgeStrokeFunction {
//                    BasicStroke(30f * (it.channelUpdates.firstOrNull()?.baseFee ?: 0) / topology.maxBaseFee)
                    BasicStroke(30f * it.channelUpdates.size / topology.maxChannelUpdateCount)
                }

                viewer.setEdgeToolTipFunction {
                    if (it != null) {
                        Toolkit.getDefaultToolkit().systemClipboard.setContents(
                            StringSelection(it.shortChannelId),
                            null
                        )
                    }
                    it?.toString()
                }

                viewer
            },
        )

        if (selectedNode != null) {
            Window(
                onCloseRequest = { selectedNode = null },
                title = selectedNode!!.id
            ) {
                NodeDetailComponent(selectedNode!!)
            }
        }
        if (selectedChannel != null) {
            Window(
                onCloseRequest = { selectedChannel = null },
                title = selectedChannel!!.shortChannelId
            ) {
                ChannelDetailComponent(selectedChannel!!)
            }
        }
    }

}