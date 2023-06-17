import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.awt.GraphicsEnvironment

// 必ず最初に適用する
fun Modifier.offsetMultiResolutionDisplay(x: Float? = null, y: Float? = null, displayScaleFactor: Double): Modifier {
    return if (x != null && y != null) {
        offset((x / displayScaleFactor).dp, (y / displayScaleFactor).dp)
    } else if (y != null) {
        offset(y = (y / displayScaleFactor).dp)
    } else if(x != null) {
        offset(x = (x / displayScaleFactor).dp)
    }else{
        offset()
    }
}

fun Modifier.widthMultiResolutionDisplay(width: Float, displayScaleFactor: Double): Modifier {
    return width((width / displayScaleFactor).dp)
}

fun Modifier.heightMultiResolutionDisplay(height: Float, displayScaleFactor: Double): Modifier {
    return height((height / displayScaleFactor).dp)
}

fun getDisplayScalingFactor(): Double {
    return GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration.defaultTransform.scaleX
}