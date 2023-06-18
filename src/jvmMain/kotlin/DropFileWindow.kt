import androidx.compose.runtime.Composable
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDropEvent
import java.io.File

@Composable
fun DropFileWindow(
    onCloseRequest: () -> Unit,
    title: String,
    onDroppedFile: (file: File) -> Unit,
    content: @Composable FrameWindowScope.() -> Unit,
) {
    Window(onCloseRequest, title = title) {
        val target = object : DropTarget() {
            @Synchronized
            override fun drop(evt: DropTargetDropEvent) {
                try {
                    evt.acceptDrop(DnDConstants.ACTION_REFERENCE)
                    val droppedFiles = evt.transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<*>
                    droppedFiles.first()?.let {
                        val file = File((it as File).absolutePath)
                        println(file.path)
                        onDroppedFile(file)
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
        window.contentPane.dropTarget = target

        content()
    }
}