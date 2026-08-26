package com.entab.phdwidget.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.entab.phdwidget.PhdWidget
import com.entab.phdwidget.PhdWidgetConfig
import com.entab.phdwidget.PhdWidgetEvent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SampleScreen()
                }
            }
        }
    }
}

@Composable
private fun SampleScreen() {
    var showHelp by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Text("Host screen content", modifier = Modifier.padding(24.dp))

        if (showHelp) {
            PhdWidget(
                config = PhdWidgetConfig(schoolCode = "DEMO_SCHOOL"),
                modifier = Modifier.fillMaxSize(),
                onEvent = { if (it is PhdWidgetEvent.Close) showHelp = false },
            )
        } else {
            FloatingActionButton(
                onClick = { showHelp = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            ) {
                Icon(Icons.Default.Chat, contentDescription = "Help")
            }
        }
    }
}
