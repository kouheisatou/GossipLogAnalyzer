package gossip_msg

data class ChannelUpdate(
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
)