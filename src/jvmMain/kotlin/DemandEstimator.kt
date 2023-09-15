import network.Direction
import network.Network
import network.Node
import java.io.File

fun estimateDemand(network: Network): Map<Node, Long> {
    for (node in network.nodes) {
        for (channel in node.value.channels) {
            for (channelUpdate in channel.edgeNode1ToNode2.channelUpdates) {
                network.demand[channel.node2] = (network.demand[channel.node2] ?: 0) + 1
            }
            for (channelUpdate in channel.edgeNode2ToNode1.channelUpdates) {
                network.demand[channel.node1] = (network.demand[channel.node1] ?: 0) + 1
            }
        }
    }
    return network.demand
}

fun printDemand(network: Network) {
    network.demand.toList().sortedByDescending { it.second }.forEach {
        println("${it.first.id}\t${it.second}")
    }
}