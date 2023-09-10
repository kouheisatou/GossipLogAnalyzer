import network.Direction
import network.Node
import java.io.File

fun estimateDemand(nodes: Map<String, Node>): Map<Node, Int> {
    val result = mutableMapOf<Node, Int>()

    for (node in nodes) {
        for (channel in node.value.channels) {
            for (channelUpdate in channel.edgeNode1ToNode2.channelUpdates) {
                result[channel.node2] = (result[channel.node2] ?: 0) + 1
            }
            for (channelUpdate in channel.edgeNode2ToNode1.channelUpdates) {
                result[channel.node1] = (result[channel.node1] ?: 0) + 1
            }
        }
    }
    return result
}