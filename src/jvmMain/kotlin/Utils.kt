fun convertShortChannelId(channelIdInt: Long): String{
    val block = channelIdInt shr 40
    val tx = (channelIdInt shr 16) and 0x0000000000FFFFFF
    val output = channelIdInt and 0x000000000000FFFF
    return "$block:$tx:$output"
}

fun main() {
    println(convertShortChannelId(857344191798837249))
}