package network

enum class Direction {
    Node1ToNode2, Node2ToNode1
}

class Edge(val channel: Channel, private val direction: Direction) {

    // channel update count
    val capacity: Int
    val sourceNode: Node
    val destinationNode: Node

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

        var count = 0
        channel.channelUpdates.forEach {
            if (it.channelFlags.endsWith(
                    when (direction) {
                        Direction.Node1ToNode2 -> "0"
                        Direction.Node2ToNode1 -> "1"
                    }
                )
            ) {
                count++
            }
        }

        capacity = count
    }

    override fun toString(): String {
        return "Edge(channel=$channel, direction=$direction, channelUpdateCount=$capacity)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Edge

        if (channel != other.channel) return false
        if (direction != other.direction) return false
        if (capacity != other.capacity) return false
        if (sourceNode != other.sourceNode) return false
        return destinationNode == other.destinationNode
    }

    override fun hashCode(): Int {
        var result = channel.hashCode()
        result = 31 * result + direction.hashCode()
        return result
    }
}