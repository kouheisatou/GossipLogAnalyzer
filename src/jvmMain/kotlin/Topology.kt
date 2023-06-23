import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.input.key.*
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import com.google.common.graph.MutableNetwork
import com.google.common.graph.NetworkBuilder
import edu.uci.ics.jung.graph.util.Graphs
import edu.uci.ics.jung.layout.algorithms.LayoutAlgorithm
import edu.uci.ics.jung.visualization.BaseVisualizationModel
import edu.uci.ics.jung.visualization.VisualizationViewer
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse
import edu.uci.ics.jung.visualization.control.ModalGraphMouse
import edu.uci.ics.jung.visualization.decorators.PickableNodePaintFunction
import edu.uci.ics.jung.visualization.picking.MultiPickedState
import edu.uci.ics.jung.visualization.picking.PickedState
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.geom.Point2D


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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TopologyComponent(
    topology: Topology,
    modifier: Modifier = Modifier
) {
    var selectedNode by remember { mutableStateOf<Node?>(null) }
    var selectedChannel by remember { mutableStateOf<Channel?>(null) }

    val viewer by remember {
        mutableStateOf(
            VisualizationViewer(
                BaseVisualizationModel(topology.g, topology.algorithm, topology.graphSize),
                topology.graphSize,
            ).apply {

                // mouse control
                val graphMouse = DefaultModalGraphMouse<Node, Channel>()
                graphMouse.setMode(ModalGraphMouse.Mode.PICKING)
                this.graphMouse = graphMouse
                this.addKeyListener(graphMouse.modeKeyListener)

                // on node clicked
                pickedNodeState.addItemListener {
                    selectedNode = it.item as Node
                }

                // on edge clicked
                pickedEdgeState.addItemListener {
                    selectedChannel = (it.item as Edge).channel
                }

                // edge stroke width
                renderContext.setEdgeStrokeFunction {
                    BasicStroke(topology.maxStrokeWidth.toFloat() * it.capacity / topology.maxEdgeCapacity)
                }

                // root node color
                renderContext.setNodeFillPaintFunction {
                    return@setNodeFillPaintFunction when (it) {
                        selectedNode -> Color.RED
                        topology.rootNode -> Color.CYAN
                        else -> Color.WHITE
                    }
                }

                // change edge color on selected
                renderContext.setEdgeDrawPaintFunction {
                    return@setEdgeDrawPaintFunction when (it.channel) {
                        selectedChannel -> Color.RED
                        else -> Color.BLACK
                    }
                }

                // node info popup
                setNodeToolTipFunction {
                    it?.id.toString()
                }

                // edge info popup
                setEdgeToolTipFunction {
                    it?.channel?.shortChannelId.toString()
                }
            }
        )
    }

    SwingPanel(
        modifier = modifier.fillMaxSize(),
        factory = { viewer },
    )

    if (selectedNode != null) {
        Window(
            onCloseRequest = { selectedNode = null },
            title = selectedNode?.id.toString(),
            onKeyEvent = {
                if (it.type == KeyEventType.KeyDown && it.key.keyCode == Key.Escape.keyCode) {
                    selectedNode = null
                }
                false
            }
        ) {
            MenuBar {
                Menu("edit") {
                    Item(
                        "Copy ID",
                        onClick = {
                            // copy to clipboard
                            val text = selectedNode?.id ?: return@Item
                            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
                            println(text)
                        },
                        shortcut = KeyShortcut(Key.C, meta = true)
                    )
                }
            }

            NodeDetailComponent(selectedNode ?: return@Window)
        }
    }

    if (selectedChannel != null) {
        Window(
            onCloseRequest = { selectedChannel = null },
            title = selectedChannel?.shortChannelId.toString(),
            onKeyEvent = {
                if (it.type == KeyEventType.KeyDown && it.key.keyCode == Key.Escape.keyCode) {
                    selectedChannel = null
                }
                false
            }
        ) {

            MenuBar {
                Menu("edit") {
                    Item(
                        "Copy ID",
                        onClick = {
                            // copy to clipboard
                            val text = selectedChannel?.shortChannelId ?: return@Item
                            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
                            println(text)
                        },
                        shortcut = KeyShortcut(Key.C, meta = true)
                    )
                }
            }

            ChannelDetailComponent(selectedChannel ?: return@Window)
        }
    }
}