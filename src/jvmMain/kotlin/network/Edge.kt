package network

import model.input_gossip_msg.ChannelUpdate

enum class Direction {
    Node1ToNode2 {
        override fun toString(): String {
            return "0"
        }
    },
    Node2ToNode1 {
        override fun toString(): String {
            return "1"
        }
    }
}

class Edge(val channel: Channel, private val direction: Direction) {

    val channelUpdates = mutableListOf<ChannelUpdate>()
    val sourceNode: Node
    val destinationNode: Node
    val counterEdge: Edge
        get() {
            return if (direction == Direction.Node1ToNode2) {
                channel.edgeNode2ToNode1
            } else {
                channel.edgeNode1ToNode2
            }
        }

    val capacity: Long?
        get() {
            return channelUpdates.lastOrNull()?.htlcMaximumMsat
        }

    init {

        when (direction) {
            Direction.Node1ToNode2 -> {
                sourceNode = channel.node1
                destinationNode = channel.node2
            }

            Direction.Node2ToNode1 -> {
                sourceNode = channel.node2
                destinationNode = channel.node1
            }
        }

    }

    override fun toString(): String {
        return "Edge(channel=$channel, direction=$direction, channelUpdate=$channelUpdates)"
    }
}