import network.Direction
import network.Network
import network.Node
import java.io.File

fun estimateDemand(network: Network, onProgressChanged: (progress: Float) -> Unit): Map<Node, Long> {
    var count = 0L
    for (node in network.nodes) {
        for (channel in node.value.channels) {
            for (channelUpdate in channel.edgeNode1ToNode2.channelUpdates) {
                network.demand[channel.node2] = (network.demand[channel.node2] ?: 0) + channel.capacity * channel.edgeNode1ToNode2.channelUpdates.size / (channel.edgeNode1ToNode2.channelUpdates.size + channel.edgeNode2ToNode1.channelUpdates.size)
            }
            for (channelUpdate in channel.edgeNode2ToNode1.channelUpdates) {
                network.demand[channel.node1] = (network.demand[channel.node1] ?: 0) + channel.capacity * channel.edgeNode2ToNode1.channelUpdates.size / (channel.edgeNode1ToNode2.channelUpdates.size + channel.edgeNode2ToNode1.channelUpdates.size)
            }
        }
        onProgressChanged(count++.toFloat() / network.nodes.size)
    }
    return network.demand
}