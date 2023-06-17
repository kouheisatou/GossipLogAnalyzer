import kotlin.math.absoluteValue

private const val HASH_MAP_SIZE = 10000

class ChannelSet {
    val channelHashMap: Array<MutableList<Channel>?> = Array(HASH_MAP_SIZE) { null }
    var channelCount = 0

    fun getHashMapIndex(channelId: String): Int{
        return channelId.hashCode().absoluteValue % HASH_MAP_SIZE
    }

    fun add(channelUpdate: ChannelUpdate) {
        val channelHolder = channelHashMap[getHashMapIndex(channelUpdate.shortChannelId)]
        if (channelHolder == null) {
            channelHashMap[getHashMapIndex(channelUpdate.shortChannelId)] =
                mutableListOf(Channel(channelUpdate.shortChannelId).apply { this.channelUpdates.add(channelUpdate) })
            channelCount++
        } else {
            var channelAlreadyExists = false
            for (c in channelHolder) {
                if (channelUpdate.shortChannelId == c.shortChannelId) {
                    c.channelUpdates.add(channelUpdate)
                    channelAlreadyExists = true
                    break
                }
            }
            if (!channelAlreadyExists) {
                channelHolder.add(Channel(channelUpdate.shortChannelId).apply { this.channelUpdates.add(channelUpdate) })
                channelCount++
            }
        }
    }

    fun get(channel: Channel): Channel? {
        val channelHolder = channelHashMap[getHashMapIndex(channel.shortChannelId)] ?: return null

        for (c in channelHolder) {
            if (channel.shortChannelId == c.shortChannelId) {
                return c
            }
        }
        return null
    }

    fun toList(): List<Channel> {
        val result = mutableListOf<Channel>()
        for (channels in channelHashMap) {
            for (channel in channels ?: listOf()) {
                result.add(channel)
            }
        }
        return result
    }
}