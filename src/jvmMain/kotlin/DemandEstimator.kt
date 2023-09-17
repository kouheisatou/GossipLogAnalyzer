import network.Direction
import network.Network
import network.Node
import java.io.File

fun estimateDemand(network: Network, onProgressChanged: (progress: Float) -> Unit): Map<Node, Long> {
    var count = 0L
    for (node in network.nodes) {
        for (channel in node.value.channels) {
            for (channelUpdate in channel.edgeNode1ToNode2.channelUpdates) {
                network.demand[channel.node2] = (network.demand[channel.node2] ?: 0) + 1
            }
            for (channelUpdate in channel.edgeNode2ToNode1.channelUpdates) {
                network.demand[channel.node1] = (network.demand[channel.node1] ?: 0) + 1
            }
        }
        onProgressChanged(count++.toFloat() / network.nodes.size)
    }
    return network.demand
}