fun estimateDemand(nodes: Map<String, Node>): Map<Node, Int> {
    val result = mutableMapOf<Node, Int>()

    for (node in nodes) {
        for (channel in node.value.channels) {
            for (channelUpdate in channel.channelUpdates) {
                if (channel.node1 == node.value && channelUpdate.direction == Direction.Node2ToNode1) {
                    result[node.value] = (result[node.value] ?: 0) + 1
                } else if (channel.node2 == node.value && channelUpdate.direction == Direction.Node1ToNode2) {
                    result[node.value] = (result[node.value] ?: 0) + 1
                }
            }
        }
    }
    return result
}