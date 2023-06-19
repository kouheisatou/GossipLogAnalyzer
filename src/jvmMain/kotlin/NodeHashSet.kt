import kotlin.math.absoluteValue

private const val HASH_MAP_SIZE = 10000

class NodeHashSet {
    private val hashMap: Array<MutableList<Node>?> = Array(HASH_MAP_SIZE) { null }

    private fun calcHashMapIndex(nodeId: String): Int {
        return Node(nodeId).hashCode().absoluteValue % HASH_MAP_SIZE
    }

    fun add(nodeId1: String, nodeId2: String, channel: Channel?) {
        val holder1 = hashMap[calcHashMapIndex(nodeId1)]
        if (holder1 == null) {
            val newNode1 = Node(nodeId1).apply { channel?.let { channels.add(it) } }
            channel?.node1 = newNode1
            hashMap[calcHashMapIndex(nodeId1)] = mutableListOf(newNode1)
        } else {
            var nodeAlreadyExists = false
            for (alreadyAddedNode in holder1) {
                if (nodeId1 == alreadyAddedNode.id) {
                    channel?.let { alreadyAddedNode.channels.add(it) }
                    channel?.node1 = alreadyAddedNode
                    nodeAlreadyExists = true
                    break
                }
            }
            if (!nodeAlreadyExists) {
                val newNode1 = Node(nodeId1).apply { channel?.let { channels.add(it) } }
                channel?.node1 = newNode1
                holder1.add(newNode1)
            }
        }

        val holder2 = hashMap[calcHashMapIndex(nodeId2)]
        if (holder2 == null) {
            val newNode2 = Node(nodeId2).apply { channel?.let { channels.add(it) } }
            channel?.node2 = newNode2
            hashMap[calcHashMapIndex(nodeId2)] = mutableListOf(newNode2)
        } else {
            var nodeAlreadyExists = false
            for (alreadyAddedNode in holder2) {
                if (nodeId2 == alreadyAddedNode.id) {
                    channel?.let { alreadyAddedNode.channels.add(it) }
                    channel?.node2 = alreadyAddedNode
                    nodeAlreadyExists = true
                    break
                }
            }
            if (!nodeAlreadyExists) {
                val newNode2 = Node(nodeId2).apply { channel?.let { channels.add(it) } }
                channel?.node2 = newNode2
                holder2.add(newNode2)
            }
        }
    }

    fun findByNodeId(nodeId: String?): Node? {
        nodeId ?: return null
        val nodeHolder = hashMap[calcHashMapIndex(nodeId)] ?: return null

        for (nodeInHolder in nodeHolder) {
            if (nodeInHolder.id == nodeId) {
                return nodeInHolder
            }
        }

        return null
    }

    fun toList(): List<Node> {
        val result = mutableListOf<Node>()
        for (nodes in hashMap) {
            var nodeIdsText = ""
            for (node in nodes ?: listOf()) {
                result.add(node)
                nodeIdsText += "${node},"
            }
        }
        return result.sortedByDescending { it.channels.size }
    }
}