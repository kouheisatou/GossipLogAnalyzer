class Topology {
    val nodeSet = mutableSetOf<TopologyNode>()
    fun build() {
        val nodesList = nodes.toList()
        val root = TopologyNode(0f, 0f, 1f, nodesList.maxBy { it.channels.size }, topology = this)
        nodeSet.add(root)
        root.build()
    }
}

class TopologyNode(val x: Float, val y: Float, val size: Float, val node: Node, topology: Topology) {
    val connectedNode = mutableSetOf<TopologyNode>()
    fun build() {

        node.channels.forEach { channel ->

            val partnerNode = when {
                channel.node1 != null && channel.node1 != node -> {
                    channel.node1!!
                }

                channel.node2 != null && channel.node2 != node -> {
                    channel.node2!!
                }

                else -> {
                    return@forEach
                }
            }

//            connectedNode.add(TopologyNode())
        }

    }

    override fun equals(other: Any?): Boolean {
        return other is TopologyNode && node == other.node
    }

    override fun hashCode(): Int {
        return node.hashCode()
    }
}