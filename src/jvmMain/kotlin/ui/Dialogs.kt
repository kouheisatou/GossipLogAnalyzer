package ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.AwtWindow
import java.awt.FileDialog
import java.awt.Frame
import java.io.FilenameFilter

@Composable
fun FilePicker(
    parent: Frame? = null,
    title: String = "Choose a file",
    mode: Int = FileDialog.LOAD,
    filenameFilter: FilenameFilter? = null,
    directory: String? = null,
    file: String? = null,
    onCloseRequest: (directory: String?, result: String?) -> Unit
) = AwtWindow(
    create = {
        object : FileDialog(parent, title, mode) {
            init {
                filenameFilter?.let { setFilenameFilter(filenameFilter) }
                directory?.let { setDirectory(directory) }
                file?.let { setFile(file) }
            }

            override fun setVisible(value: Boolean) {
                super.setVisible(value)
                if (value) {
                    onCloseRequest(this.directory, this.file)
                }
            }
        }
    },
    dispose = FileDialog::dispose
)