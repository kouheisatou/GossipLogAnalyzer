package model.gossip_msg

import network.Direction
import java.lang.Exception

class ChannelUpdate(
    val sig: String,
    val chainHash: String,
    val shortChannelId: String,
    val timestamp: Long,
    val messageFlags: String,
    val channelFlags: String,
    val timeLockDelta: String,
    val htlcMinimumMsat: Long,
    val baseFee: Long,
    val feeRate: Long,
    val htlcMaximumMsat: Long,
) {
    val direction: Direction
        get() {
            return if (channelFlags.endsWith("0")) {
                Direction.Node1ToNode2
            } else if (channelFlags.endsWith("1")) {
                Direction.Node2ToNode1
            } else {
                throw Exception("invalid channel flag")
            }
        }

    override fun toString(): String {
        return "ChannelUpdate(sig='$sig', chainHash='$chainHash', shortChannelId='$shortChannelId', timestamp=$timestamp, messageFlags='$messageFlags', channelFlags='$channelFlags', timeLockDelta='$timeLockDelta', htlcMinimumMsat=$htlcMinimumMsat, baseFee=$baseFee, feeRate=$feeRate, htlcMaximumMsat=$htlcMaximumMsat)"
    }
}