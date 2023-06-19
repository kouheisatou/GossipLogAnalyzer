package gossip_msg

data class ChannelUpdate(
    val sig: String,
    val chainHash: String,
    val shortChannelId: String,
    val timestamp: String,
    val messageFlags: String,
    val channelFlags: String,
    val timeLockDelta: String,
    val htlcMinimumMsat: Float,
    val baseFee: Int,
    val feeRate: Int,
    val htlcMaximumMsat: Float,
)