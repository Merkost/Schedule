package ru.dvfu.appliances.platform

import androidx.compose.runtime.Composable

interface ImagePickerLauncher {
    fun launch()
}

@Composable
expect fun rememberImagePickerLauncher(
    onImageSelected: (ByteArray) -> Unit,
): ImagePickerLauncher
