package com.entab.phdwidget

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Self-contained chat screen — launch it with [start] and it owns its whole lifecycle,
 * including Back and closing, the same way Yellow.ai's own Android SDK launches a dedicated
 * Activity rather than asking the host app to embed and wire up a View. No `onEvent` glue
 * required.
 *
 * Use [PhdWidget] instead only if you specifically need to overlay the chat inside an
 * existing screen (e.g. a persistent support FAB over live content).
 */
class PhdChatActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = PhdWidgetConfig(
            schoolCode = intent.getStringExtra(EXTRA_SCHOOL_CODE) ?: "",
            host = intent.getStringExtra(EXTRA_HOST) ?: "https://genie.globalmetaldirect.com",
            extraConfig = readExtraConfig(intent),
            autoOpen = intent.getBooleanExtra(EXTRA_AUTO_OPEN, true),
        )

        setContent {
            var failure by remember { mutableStateOf<String?>(null) }
            var reloadKey by remember { mutableStateOf(0) }

            val currentFailure = failure
            if (currentFailure != null) {
                FailureScreen(
                    description = currentFailure,
                    onRetry = { failure = null; reloadKey++ },
                )
            } else {
                // key() forces a fresh PhdWidget (and thus a fresh WebView load) on retry —
                // PhdWidgetConfig is unchanged, so recomposition alone would reuse the same
                // failed WebView instance.
                key(reloadKey) {
                    PhdWidget(
                        config = config,
                        modifier = Modifier.fillMaxSize(),
                        onEvent = { event ->
                            when (event) {
                                is PhdWidgetEvent.Close -> finish()
                                is PhdWidgetEvent.Failed -> failure = event.description
                                else -> Unit
                            }
                        },
                    )
                }
            }
        }
    }

    companion object {
        private const val EXTRA_SCHOOL_CODE = "com.entab.phdwidget.SCHOOL_CODE"
        private const val EXTRA_HOST = "com.entab.phdwidget.HOST"
        private const val EXTRA_EXTRA_CONFIG = "com.entab.phdwidget.EXTRA_CONFIG"
        private const val EXTRA_AUTO_OPEN = "com.entab.phdwidget.AUTO_OPEN"

        /** Launches the chat as its own screen — Back and close are handled internally,
         *  unlike [PhdWidget] which requires the caller to react to [PhdWidgetEvent.Close]. */
        @JvmStatic
        fun start(context: Context, config: PhdWidgetConfig) {
            val intent = Intent(context, PhdChatActivity::class.java).apply {
                putExtra(EXTRA_SCHOOL_CODE, config.schoolCode)
                putExtra(EXTRA_HOST, config.host)
                putExtra(EXTRA_EXTRA_CONFIG, HashMap(config.extraConfig))
                putExtra(EXTRA_AUTO_OPEN, config.autoOpen)
            }
            context.startActivity(intent)
        }

        @Suppress("DEPRECATION", "UNCHECKED_CAST")
        private fun readExtraConfig(intent: Intent): Map<String, String> =
            (intent.getSerializableExtra(EXTRA_EXTRA_CONFIG) as? HashMap<String, String>) ?: emptyMap()
    }
}

@Composable
private fun FailureScreen(description: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.align(Alignment.Center).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BasicText("Couldn't load chat")
            BasicText(description)
            BasicText(
                "Retry",
                modifier = Modifier.clickable(onClick = onRetry).padding(top = 8.dp),
            )
        }
    }
}
