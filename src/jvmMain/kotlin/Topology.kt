import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.window.Window
import com.google.common.graph.MutableNetwork
import com.google.common.graph.NetworkBuilder
import edu.uci.ics.jung.graph.util.Graphs
import edu.uci.ics.jung.layout.algorithms.LayoutAlgorithm
import edu.uci.ics.jung.layout.algorithms.StaticLayoutAlgorithm
import edu.uci.ics.jung.visualization.BaseVisualizationModel
import edu.uci.ics.jung.visualization.VisualizationViewer
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse
import edu.uci.ics.jung.visualization.control.ModalGraphMouse
import java.awt.*
import java.awt.datatransfer.StringSelection


class Topology(
    val graphSize: Dimension,
    val maxStrokeWidth: Int,
    val algorithm: LayoutAlgorithm<Node>,
) {
    val g: MutableNetwork<Node, Channel> = Graphs.synchronizedNetwork(
        NetworkBuilder
            .directed()
            .allowsParallelEdges(true)
            .allowsSelfLoops(true)
            .build()
    )

    var maxBaseFee = 0
    var maxChannelUpdateCount = 0

    var rootNode: Node? = null

    // overall graph
    constructor(
        graphSize: Dimension,
        maxStrokeWidth: Int,
        algorithm: LayoutAlgorithm<Node>,
        channels: ChannelHashSet
    ) : this(
        graphSize,
        maxStrokeWidth,
        algorithm
    ) {
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

    // graph within `depth` hop
    constructor(
        graphSize: Dimension,
        maxStrokeWidth: Int,
        algorithm: LayoutAlgorithm<Node>,
        node: Node,
        depth: Int
    ) : this(
        graphSize,
        maxStrokeWidth,
        algorithm
    ) {
        rootNode = node
        var currentDepth = 0

        fun build(node: Node) {

            currentDepth++
            if (currentDepth > depth) return

            node.channels.forEach { channel ->

                if (channel.channelUpdates.size > maxChannelUpdateCount) {
                    maxChannelUpdateCount = channel.channelUpdates.size
                }
                channel.channelUpdates.forEach {
                    if (it.baseFee > maxBaseFee) {
                        maxBaseFee = it.baseFee
                    }
                }

                if (channel.node2 != node && channel.node2 != null) {
                    g.addEdge(node, channel.node2!!, channel)
                    build(channel.node2!!)
                } else if (channel.node1 != node && channel.node1 != null) {
                    g.addEdge(channel.node1!!, node, channel)
                    build(channel.node1!!)
                }
            }
        }

        build(node)
    }
}

@Composable
fun TopologyComponent(
    topology: Topology,
    modifier: Modifier = Modifier
) {

    SwingPanel(
        modifier = modifier.fillMaxSize(),
        factory = {
            val viewer = VisualizationViewer(
                BaseVisualizationModel(
                    topology.g,
                    topology.algorithm,
                    topology.graphSize
                ),
                topology.graphSize,
            )

            // mouse control
            viewer.graphMouse = DefaultModalGraphMouse<Node, Channel>().apply {
                setMode(ModalGraphMouse.Mode.TRANSFORMING)
            }

            // node info popup
            viewer.setNodeToolTipFunction {
                if (it != null) {
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(it.id), null)
                }
                it?.toString()
            }

            // edge stroke width
            viewer.renderContext.setEdgeStrokeFunction {
//                    BasicStroke(30f * (it.channelUpdates.firstOrNull()?.baseFee ?: 0) / topology.maxBaseFee)
                BasicStroke(topology.maxStrokeWidth.toFloat() * it.channelUpdates.size / topology.maxChannelUpdateCount)
            }

            // root node color
            viewer.renderContext.setNodeFillPaintFunction {
                return@setNodeFillPaintFunction if (it == topology.rootNode) {
                    Color.CYAN
                } else {
                    Color.RED
                }
            }

            // edge info popup
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
}