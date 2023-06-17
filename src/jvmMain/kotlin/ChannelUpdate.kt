class ChannelUpdate(
    val sig: String,
    val chainHash: String,
    val shortChannelId: String,
    val timestamp: String,
    val messageFlags: String,
    val channelFlags: String,
    val timeLockDelta: String,
    val htlcMinimumMsat: Float,
    val baseFee: String,
    val feeRate: String,
    val htlcMaximumMsat: Float,
) {
    override fun toString(): String {
        return "ChannelUpdate(sig='$sig', chainHash='$chainHash', shortChannelId='$shortChannelId', timestamp='$timestamp', messageFlags='$messageFlags', channelFlags='$channelFlags', timeLockDelta='$timeLockDelta', htlcMinimumMsat='$htlcMinimumMsat', baseFee='$baseFee', feeRate='$feeRate', htlcMaximumMsat='$htlcMaximumMsat')"
    }
}