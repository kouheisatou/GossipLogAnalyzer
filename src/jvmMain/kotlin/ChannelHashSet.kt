import gossip_msg.ChannelUpdate
import kotlin.math.absoluteValue

private const val HASH_MAP_SIZE = 100000

class ChannelHashSet {
    private val hashMap: Array<MutableList<Channel>?> = Array(HASH_MAP_SIZE) { null }

    private fun getHashMapIndex(channelId: String): Int{
        return channelId.hashCode().absoluteValue % HASH_MAP_SIZE
    }

    fun add(channelUpdate: ChannelUpdate) {
        val channelHolder = hashMap[getHashMapIndex(channelUpdate.shortChannelId)]
        if (channelHolder == null) {
            hashMap[getHashMapIndex(channelUpdate.shortChannelId)] =
                mutableListOf(Channel(channelUpdate.shortChannelId).apply { this.channelUpdates.add(channelUpdate) })
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
            }
        }
    }

    fun get(channelId: String): Channel? {
        val channelHolder = hashMap[getHashMapIndex(channelId)] ?: return null

        for (channel in channelHolder) {
            if (channelId == channel.shortChannelId) {
                return channel
            }
        }
        return null
    }

    fun toList(): List<Channel> {
        val result = mutableListOf<Channel>()
        println("index\tused\tchannelIds")
        var channelUpdateCount = 0
        for (channels in hashMap.withIndex()) {
            var channelIds = ""
            for (channel in channels.value ?: listOf()) {
                result.add(channel)
                channelIds += "${channel.shortChannelId}(${channel.channelUpdates.size}),"
                channelUpdateCount+=channel.channelUpdates.size
            }
            println("${channels.index}\t${channels.value?.size ?: 0}\t$channelIds")
        }
        println("count of channel_update : $channelUpdateCount")
        return result.sortedByDescending { it.channelUpdates.size }
    }
}