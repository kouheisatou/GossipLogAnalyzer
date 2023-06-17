class Channel(
    val shortChannelId: String,
) {
    val channelUpdates: MutableList<ChannelUpdate> = mutableListOf()

    override fun equals(other: Any?): Boolean {
        return other is Channel && other.shortChannelId == this.shortChannelId
    }

    override fun hashCode(): Int {
        var result = shortChannelId.hashCode()
        result = 31 * result + channelUpdates.hashCode()
        return result
    }

    override fun toString(): String {
        return "Channel(shortChannelId='$shortChannelId', channelUpdates=$channelUpdates)"
    }

}