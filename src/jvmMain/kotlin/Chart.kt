import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize

class ChartDataEntry(val x: Float, val y: Float, val label: String) {
    override fun toString(): String {
        return "ChartDataEntry(x=$x, y=$y, label='$label')"
    }
}

class ChartData(
    data: List<ChartDataEntry>,
    val chartPaddingRatio: Float,
    val plotSize: Float,
) {
    val data: List<ChartDataEntry>
    val minX: Float
    val minY: Float
    val maxX: Float
    val maxY: Float
    val paddingX: Float
    val paddingY: Float

    init {
        this.data = data.sortedBy { it.x }
        var tempMinX: Float? = null
        var tempMinY: Float? = null
        var tempMaxX: Float? = null
        var tempMaxY: Float? = null
        data.forEach {
            if ((tempMinX ?: Float.MAX_VALUE) > it.x) {
                tempMinX = it.x
            }
            if ((tempMinY ?: Float.MAX_VALUE) > it.x) {
                tempMinY = it.y
            }
            if ((tempMaxX ?: Float.MIN_VALUE) < it.y) {
                tempMaxX = it.x
            }
            if ((tempMaxY ?: Float.MIN_VALUE) < it.y) {
                tempMaxY = it.y
            }
        }

        if (tempMinX == null || tempMinY == null || tempMaxX == null || tempMaxY == null) {
            minX = 0f
            minY = 0f
            maxX = 10f
            maxY = 10f
        } else {

            minX = tempMinX!!
            minY = tempMinY!!
            maxX = if (tempMaxX == tempMinX) {
                tempMaxX!! + 10f
            } else {
                tempMaxX!!
            }
            maxY = if (tempMaxY == tempMinY) {
                tempMaxY!! + 10f
            } else {
                tempMaxY!!
            }
        }

        paddingY = chartPaddingRatio * (maxY - minY)
        paddingX = chartPaddingRatio * (maxY - minX)
    }

    override fun toString(): String {
        return "ChartData(data=$data, minX=$minX, minY=$minY, maxX=$maxX, maxY=$maxY)"
    }

    fun calcCoordinateOnDisplayX(x: Float, chartWidth: Float): Float {
        return (x - minX + paddingX) / (maxX - minX + paddingX * 2) * chartWidth - (plotSize / 2)
    }

    fun calcCoordinateOnDisplayY(y: Float, chartHeight: Float): Float {
        return chartHeight - ((y - minY + paddingY) / (maxY - minY + paddingY * 2) * chartHeight - (plotSize / 2))
    }
}

@Composable
fun Chart(
    chartData: ChartData,
    modifier: Modifier = Modifier,
) {
    var chartSize by remember { mutableStateOf<IntSize>(IntSize.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { chartSize = it }
            .drawBehind {
                var prevData: ChartDataEntry? = null
                chartData.data.forEach {

                    // plot
                    drawRect(
                        color = Color.Red,
                        topLeft = Offset(
                            chartData.calcCoordinateOnDisplayX(it.x, size.width),
                            chartData.calcCoordinateOnDisplayY(it.y, size.height)
                        ),
                        size = Size(chartData.plotSize, chartData.plotSize)
                    )

                    // connecting line
                    if (prevData != null) {
                        drawLine(
                            color = Color.Red,
                            start = Offset(
                                chartData.calcCoordinateOnDisplayX(prevData!!.x, size.width),
                                chartData.calcCoordinateOnDisplayY(prevData!!.y, size.height)
                            ),
                            end = Offset(
                                chartData.calcCoordinateOnDisplayX(it.x, size.width),
                                chartData.calcCoordinateOnDisplayY(it.y, size.height)
                            ),
                            strokeWidth = chartData.plotSize / 3
                        )

                    }
                    prevData = it
                }

                // x axis
                drawLine(
                    color = Color.Black,
                    start = Offset(
                        chartData.calcCoordinateOnDisplayX(chartData.minX - chartData.paddingX, size.width),
                        chartData.calcCoordinateOnDisplayY(0f, size.height)
                    ),
                    end = Offset(
                        chartData.calcCoordinateOnDisplayX(chartData.maxX + chartData.paddingX, size.width),
                        chartData.calcCoordinateOnDisplayY(0f, size.height)
                    ),
                    strokeWidth = chartData.plotSize / 3
                )

                // y axis
                drawLine(
                    color = Color.Black,
                    start = Offset(
                        chartData.calcCoordinateOnDisplayX(0f, size.width),
                        chartData.calcCoordinateOnDisplayY(chartData.minY - chartData.paddingY, size.height)
                    ),
                    end = Offset(
                        chartData.calcCoordinateOnDisplayX(0f, size.width),
                        chartData.calcCoordinateOnDisplayY(chartData.maxY + chartData.paddingY, size.height)
                    ),
                    strokeWidth = 0.3f
                )
            }
    )
}

@Preview
@Composable
fun ChartPreview() {
//    val chartData = ChartData(
//        listOf(
//            ChartDataEntry(0f, 0f, "a"),
//            ChartDataEntry(0.1f, 0.3f, "b"),
//            ChartDataEntry(0.3f, 0.5f, "b"),
//            ChartDataEntry(0.4f, 0.3f, "b"),
//            ChartDataEntry(0.6f, 1.4f, "b"),
//            ChartDataEntry(0.7f, 1.1f, "b"),
//            ChartDataEntry(0.5f, 1.2f, "c"),
//            ChartDataEntry(0.8f, 1.2f, "c"),
//            ChartDataEntry(1.2f, 1.2f, "c"),
//            ChartDataEntry(1.1f, 1.0f, "c"),
//            ChartDataEntry(1.4f, 1.2f, "c"),
//            ChartDataEntry(1.5f, 1.3f, "c"),
//            ChartDataEntry(1.6f, 1.4f, "c"),
//            ChartDataEntry(1.7f, 1.5f, "c"),
//            ChartDataEntry(1.9f, 1.4f, "c"),
//            ChartDataEntry(2.1f, 2.1f, "c"),
//            ChartDataEntry(2f, 1f, "d"),
//        ), 0.1f, 2f
//    )
    val chartData = ChartData(
        listOf(
            ChartDataEntry(0f, 1f, "a"),
            ChartDataEntry(0.1f, 1f, "b"),
            ChartDataEntry(0.3f, 1f, "b"),
            ChartDataEntry(0.4f, 1f, "b"),
            ChartDataEntry(2f, 2f, "d"),
        ), 0.1f, 2f
    )
    println(chartData)

    Chart(chartData)
}