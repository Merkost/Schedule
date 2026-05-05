package ru.dvfu.appliances.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import platform.Foundation.NSData
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.darwin.NSObject
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberImagePickerLauncher(
    onImageSelected: (ByteArray) -> Unit,
): ImagePickerLauncher {
    val callback = remember { onImageSelected }
    return remember {
        object : ImagePickerLauncher {
            override fun launch() {
                val config = PHPickerConfiguration()
                config.filter = PHPickerFilter.imagesFilter
                config.selectionLimit = 1
                val picker = PHPickerViewController(configuration = config)
                val delegate = object : NSObject(), PHPickerViewControllerDelegateProtocol {
                    override fun picker(
                        picker: PHPickerViewController,
                        didFinishPicking: List<*>,
                    ) {
                        picker.dismissViewControllerAnimated(true, null)
                        val result = didFinishPicking.firstOrNull() as? PHPickerResult ?: return
                        result.itemProvider.loadDataRepresentationForTypeIdentifier(
                            "public.jpeg",
                        ) { data, _ ->
                            val nsData = data as? NSData ?: return@loadDataRepresentationForTypeIdentifier
                            val bytes = ByteArray(nsData.length.toInt())
                            if (bytes.isNotEmpty()) {
                                memcpy(bytes.refTo(0), nsData.bytes, nsData.length)
                            }
                            callback(bytes)
                        }
                    }
                }
                picker.delegate = delegate
                UIApplication.sharedApplication.keyWindow
                    ?.rootViewController
                    ?.presentViewController(picker, animated = true, completion = null)
            }
        }
    }
}
