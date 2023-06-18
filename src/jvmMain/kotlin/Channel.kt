import gossip_msg.ChannelUpdate

class Channel(
    val shortChannelId: String,
) {
    var node1: Node? = null
    var node2: Node? = null
    val channelUpdates: MutableList<ChannelUpdate> = mutableListOf()

    override fun equals(other: Any?): Boolean {
        return other is Channel && other.shortChannelId == this.shortChannelId
    }

    override fun hashCode(): Int {
        return shortChannelId.hashCode()
    }

    override fun toString(): String {
        return "Channel(shortChannelId='$shortChannelId', node1=${node1?.id}, node2=${node2?.id}, channelUpdates=$channelUpdates)"
    }

}