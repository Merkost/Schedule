package ru.dvfu.appliances.main

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    PlaceholderApp()
}

@Composable
private fun PlaceholderApp() {
    Surface(color = MaterialTheme.colorScheme.background) {
        Text(
            text = "Schedule iOS — migration in progress",
            style = MaterialTheme.typography.titleLarge,
        )
    }
}
