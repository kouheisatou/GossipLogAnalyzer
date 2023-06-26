import gossip_msg.ChannelUpdate
import java.io.File
import kotlin.math.absoluteValue

private const val HASH_MAP_SIZE = 100000

class ChannelHashSet {
    private val hashMap: Array<MutableList<Channel>?> = Array(HASH_MAP_SIZE) { null }

    private fun getHashMapIndex(channelId: String): Int {
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

    fun findChannelById(channelId: String?): Channel? {
        channelId ?: return null
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
        var channelUpdateCount = 0
        for (channels in hashMap) {
            for (channel in channels ?: listOf()) {
                result.add(channel)
                channelUpdateCount += channel.channelUpdates.size
            }
        }
        return result.sortedByDescending { it.channelUpdates.size }
    }

    fun reset() {
        for (i in hashMap.indices) {
            hashMap[i] = null
        }
    }
}
