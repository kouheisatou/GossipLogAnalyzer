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
import edu.uci.ics.jung.layout.spatial.Circle
import edu.uci.ics.jung.visualization.BaseVisualizationModel
import edu.uci.ics.jung.visualization.MultiLayerTransformer
import edu.uci.ics.jung.visualization.VisualizationViewer
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse
import edu.uci.ics.jung.visualization.control.ModalGraphMouse
import network.*
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D


class Topology(
    val graphSize: Dimension,
    val maxStrokeWidth: Int,
    val algorithm: LayoutAlgorithm<Node>,
    val nodes: Map<String, Node>,
) {
    val g: MutableNetwork<Node, Edge> = Graphs.synchronizedNetwork(
        NetworkBuilder
            .directed()
            .allowsParallelEdges(true)
            .allowsSelfLoops(true)
            .build()
    )
    val model = BaseVisualizationModel(g, algorithm, graphSize)

    var maxEdgeCapacity = 0

    var rootNode: Node? = null
    var selectedNode = mutableStateOf<Node?>(null)
    var selectedChannel = mutableStateOf<Channel?>(null)

    val estimatedDemand = estimateDemand(this.nodes)

    // overall graph
    constructor(
        graphSize: Dimension,
        maxStrokeWidth: Int,
        algorithm: LayoutAlgorithm<Node>,
        nodes: Map<String, Node>,
        channels: Map<String, Channel>,
    ) : this(
        graphSize,
        maxStrokeWidth,
        algorithm,
        nodes
    ) {
        for ((_, channel) in channels) {
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

    // graph within `depth` hop
    constructor(
        graphSize: Dimension,
        maxStrokeWidth: Int,
        algorithm: LayoutAlgorithm<Node>,
        nodes: Map<String, Node>,
        rootNode: Node,
        maxDepth: Int
    ) : this(
        graphSize,
        maxStrokeWidth,
        algorithm,
        nodes
    ) {

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

                    if (channel.node2 != node) {
                        build(channel.node2, depth + 1)
                    } else if (channel.node1 != node) {
                        build(channel.node1, depth + 1)
                    }
                }
            }
        }

        build(rootNode, 0)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TopologyComponent(
    topology: Topology,
    modifier: Modifier = Modifier
) {
    val viewer = VisualizationViewer(
        topology.model,
        topology.graphSize,
    ).apply {

        // mouse control
        graphMouse = DefaultModalGraphMouse<Node, Channel>().apply {
            setMode(ModalGraphMouse.Mode.PICKING)
        }

        // on node clicked
        pickedNodeState.addItemListener {
            if (it.item == null) return@addItemListener

            topology.selectedNode.value = it.item as Node
        }

        // on edge clicked
        pickedEdgeState.addItemListener {
            if (it.item == null) return@addItemListener

            topology.selectedChannel.value = (it.item as Edge).channel
        }

        // edge stroke width
        renderContext.setEdgeStrokeFunction {
            BasicStroke(topology.maxStrokeWidth.toFloat() * it.capacity / topology.maxEdgeCapacity)
        }

        // node size
        renderContext.setNodeShapeFunction {
            Circle(edu.uci.ics.jung.layout.model.Point.of(0.0, 0.0), 0.2)
            Rectangle2D.Float(
                0f,
                0f,
                (topology.estimatedDemand[it]?.toFloat() ?: 0f) + 1f,
                (topology.estimatedDemand[it]?.toFloat() ?: 0f) + 1f,
            )
        }

        // root node color
        renderContext.setNodeFillPaintFunction {
            return@setNodeFillPaintFunction when (it) {
                topology.selectedNode.value -> Color.RED
                topology.rootNode -> Color.CYAN
                else -> Color.WHITE
            }
        }

        // change edge color on selected
        renderContext.setEdgeDrawPaintFunction {
            return@setEdgeDrawPaintFunction when (it.channel) {
                topology.selectedChannel.value -> Color.RED
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

    LaunchedEffect(topology) {

        viewer.renderContext
            .multiLayerTransformer
            .getTransformer(MultiLayerTransformer.Layer.VIEW)
            .setToIdentity()
        if (topology.rootNode != null) {
            println("(${viewer.model.layoutModel.get(topology.rootNode).x}, ${viewer.model.layoutModel.get(topology.rootNode).y})")

            val targetPoint = Point2D.Double(
                viewer.model.layoutModel.get(topology.rootNode).x,
                viewer.model.layoutModel.get(topology.rootNode).y
            )
            viewer.renderContext
                .multiLayerTransformer
                .getTransformer(MultiLayerTransformer.Layer.VIEW)
                .translate(
                    targetPoint.x * -1 + viewer.size.width.toDouble() / 2,
                    targetPoint.y * -1 + viewer.size.height.toDouble() / 2
                )
        } else {
            viewer.renderContext
                .multiLayerTransformer
                .getTransformer(MultiLayerTransformer.Layer.VIEW)
                .translate(viewer.size.width.toDouble() / -2, viewer.size.height.toDouble() / -2)
        }
    }

    SwingPanel(
        modifier = modifier.fillMaxSize(),
        factory = { viewer },
    )

    if (topology.selectedNode.value != null) {
        Window(
            onCloseRequest = { topology.selectedNode.value = null },
            title = "Node " + topology.selectedNode.value?.id.toString(),
            onKeyEvent = {
                if (it.type == KeyEventType.KeyDown && it.key.keyCode == Key.Escape.keyCode) {
                    topology.selectedNode.value = null
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
                            val text = topology.selectedNode.value?.id ?: return@Item
                            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
                            println(text)
                        },
                        shortcut = KeyShortcut(Key.C, meta = true)
                    )
                }
            }

            NodeDetailComponent(topology.selectedNode.value ?: return@Window)
        }
    }

    if (topology.selectedChannel.value != null) {
        Window(
            onCloseRequest = { topology.selectedChannel.value = null },
            title = "Channel " + topology.selectedChannel.value?.shortChannelId.toString(),
            onKeyEvent = {
                if (it.type == KeyEventType.KeyDown && it.key.keyCode == Key.Escape.keyCode) {
                    topology.selectedChannel.value = null
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
                            val text = topology.selectedChannel.value?.shortChannelId ?: return@Item
                            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
                            println(text)
                        },
                        shortcut = KeyShortcut(Key.C, meta = true)
                    )
                }
            }

            ChannelDetailComponent(topology.selectedChannel.value ?: return@Window)
        }
    }
}