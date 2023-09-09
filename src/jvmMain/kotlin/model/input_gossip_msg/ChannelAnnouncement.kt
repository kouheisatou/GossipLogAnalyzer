package model.gossip_msg

data class ChannelAnnouncement(
    val nodeSig1: String,
    val nodeSig2: String,
    val bitcoinSig1: String,
    val bitcoinSig2: String,
    val features: String,
    val chainHash: String,
    val shortChannelId: String,
    val nodeId1: String,
    val nodeId2: String,
    val bitcoinKey1: String,
    val bitcoinKey2: String,
)