package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun downloadTaskCard_screenshot() {
    val sampleTask = DownloadTask(
      url = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf",
      fileName = "tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf",
      status = DownloadTaskStatus.DOWNLOADING,
      progress = 45f,
      downloadedBytes = 300_000_000L,
      totalBytes = 669_000_000L,
      speedBytesPerSec = 4_200_000L
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        DownloadTaskCard(
          task = sampleTask,
          queuePosition = 1,
          onCancel = {},
          onRetry = {},
          onRemove = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/download_task.png")
  }
}

