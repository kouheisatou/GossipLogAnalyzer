import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import com.google.common.graph.MutableNetwork
import com.google.common.graph.NetworkBuilder
import edu.uci.ics.jung.graph.util.Graphs
import edu.uci.ics.jung.layout.algorithms.LayoutAlgorithm
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
    val g: MutableNetwork<Node, Edge> = Graphs.synchronizedNetwork(
        NetworkBuilder
            .directed()
            .allowsParallelEdges(true)
            .allowsSelfLoops(true)
            .build()
    )

    var maxEdgeCapacity = 0

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
                val edge1To2 = Edge(channel, Direction.Node1ToNode2)
                val edge2To1 = Edge(channel, Direction.Node2ToNode1)

                if (edge1To2.capacity > maxEdgeCapacity) {
                    maxEdgeCapacity = edge1To2.capacity
                }
                if (edge2To1.capacity > maxEdgeCapacity) {
                    maxEdgeCapacity = edge2To1.capacity
                }

                if (edge1To2.capacity > 0) g.addEdge(edge1To2.sourceNode, edge1To2.destinationNode, edge1To2)
                if (edge2To1.capacity > 0) g.addEdge(edge2To1.sourceNode, edge2To1.destinationNode, edge2To1)
            }
        }
    }

    // graph within `depth` hop
    constructor(
        graphSize: Dimension,
        maxStrokeWidth: Int,
        algorithm: LayoutAlgorithm<Node>,
        node: Node,
        maxDepth: Int
    ) : this(
        graphSize,
        maxStrokeWidth,
        algorithm
    ) {
        rootNode = node

        fun build(node: Node, depth: Int) {

            if (maxDepth < depth) return

            node.channels.forEach { channel ->

                val edge1To2 = Edge(channel, Direction.Node1ToNode2)
                val edge2To1 = Edge(channel, Direction.Node2ToNode1)

                if (edge1To2.capacity > maxEdgeCapacity) {
                    maxEdgeCapacity = edge1To2.capacity
                }
                if (edge2To1.capacity > maxEdgeCapacity) {
                    maxEdgeCapacity = edge2To1.capacity
                }

                if (!g.edges().contains(edge1To2) && !g.edges().contains(edge2To1)) {
                    if (edge1To2.capacity > 0) g.addEdge(edge1To2.sourceNode, edge1To2.destinationNode, edge1To2)
                    if (edge2To1.capacity > 0) g.addEdge(edge2To1.sourceNode, edge2To1.destinationNode, edge2To1)

                    if (channel.node2 != node && channel.node2 != null) {
                        build(channel.node2!!, depth + 1)
                    } else if (channel.node1 != node && channel.node1 != null) {
                        build(channel.node1!!, depth + 1)
                    }
                }
            }
        }

        build(node, 0)
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
                BasicStroke(topology.maxStrokeWidth.toFloat() * it.capacity / topology.maxEdgeCapacity)
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
                        StringSelection(it.channel.shortChannelId),
                        null
                    )
                }
                it?.toString()
            }

            viewer
        },
    )
}