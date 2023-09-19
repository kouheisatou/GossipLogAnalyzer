fun convertShortChannelId(channelIdInt: Long): String {
    val block = channelIdInt shr 40
    val tx = (channelIdInt shr 16) and 0x0000000000FFFFFF
    val output = channelIdInt and 0x000000000000FFFF
    return "$block:$tx:$output"
}

fun <T> movingAverage(
    data: List<T>,
    horizontalAxisValue: (element: T) -> Long,
    verticalAxisValue: (element: T) -> Long,
    windowSize: Long
): List<Pair<Long, Float>> {
    val result = mutableListOf<Pair<Long, Float>>()
    data.forEach { element ->
        val from: Long
        val to: Long
        if (windowSize % 2 != 0L) {
            from = horizontalAxisValue(element) - windowSize / 2
            to = horizontalAxisValue(element) + windowSize / 2
        } else {
            from = horizontalAxisValue(element) - windowSize / 2
            to = horizontalAxisValue(element) + windowSize / 2 - 1
        }

        val window = mutableListOf<T>()
        data.forEach {
            if (horizontalAxisValue(it) in from..to) {
                window.add(it)
            }
        }

        var sum = 0L
        window.forEach {
            sum += verticalAxisValue(it)
        }
        val ave = sum.toFloat() / window.size
        result.add(Pair(horizontalAxisValue(element), ave))
    }
    return result
}

fun main() {
    println(convertShortChannelId(857344191798837249))
}