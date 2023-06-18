import kotlin.math.absoluteValue

private const val HASH_MAP_SIZE = 10000

class NodeHashSet {
    private val hashMap: Array<MutableList<Node>?> = Array(HASH_MAP_SIZE) { null }

    private fun getHashMapIndex(node: Node): Int {
        return node.hashCode().absoluteValue % HASH_MAP_SIZE
    }

    fun add(node: Node, channel: Channel) {
        val nodeHolder = hashMap[getHashMapIndex(node)]
        if (nodeHolder == null) {
            hashMap[getHashMapIndex(node)] = mutableListOf(node.apply { channels.add(channel) })
        } else {
            var nodeAlreadyExists = false
            for (nodeInHolder in nodeHolder) {
                if (node == nodeInHolder) {
                    if(!nodeInHolder.channels.contains(channel)) {
                        nodeInHolder.channels.add(channel)
                    }
                    nodeAlreadyExists = true
                    break
                }
            }
            if (!nodeAlreadyExists) {
                nodeHolder.add(node.apply { channels.add(channel) })
            }
        }
    }

    fun find(node: Node): Node? {
        val nodeHolder = hashMap[getHashMapIndex(node)] ?: return null

        for (nodeInHolder in nodeHolder) {
            if (nodeInHolder == node) {
                return nodeInHolder
            }
        }
        return null
    }

    fun toList(): List<Node> {
        val result = mutableListOf<Node>()
//        println("index\tused\tchannelIds")
        for (nodes in hashMap.withIndex()) {
            var nodeIdsText = ""
            for (node in nodes.value ?: listOf()) {
                result.add(node)
                nodeIdsText += "${node.id},"
            }
//            println("${nodes.index}\t${nodes.value?.size ?: 0}\t$nodeIdsText")
        }
        return result.sortedByDescending { it.channels.size }
    }
}