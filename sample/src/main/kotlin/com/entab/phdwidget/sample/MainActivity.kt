package com.entab.phdwidget.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
                    LoginScreenWithChat()
                }
            }
        }
    }
}

/**
 * A realistic host screen — a login form with real text fields — proving that the on-demand
 * mount pattern (FAB toggles a boolean; the widget is only in the tree while open) does not
 * interfere with the rest of the screen. The chat FAB sits bottom-right; text fields keep
 * working normally whether the widget has ever been opened or not, because the WebView is
 * only added to the hierarchy while [showChat] is true.
 */
@Composable
private fun LoginScreenWithChat() {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showChat by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Sign in", style = MaterialTheme.typography.headlineSmall)

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(onClick = { /* your auth flow */ }, modifier = Modifier.fillMaxWidth()) {
                Text("Log in")
            }
        }

        if (showChat) {
            PhdWidget(
                config = PhdWidgetConfig(schoolCode = "DEMO_SCHOOL"),
                modifier = Modifier.fillMaxSize(),
                // REQUIRED: PhdWidget does not unmount itself. System Back emits Close —
                // if you don't flip your own state here, Back will appear to do nothing
                // while the widget is open.
                onEvent = { if (it is PhdWidgetEvent.Close) showChat = false },
            )
        } else {
            FloatingActionButton(
                onClick = { showChat = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .systemBarsPadding()
                    .padding(16.dp),
            ) {
                Icon(Icons.Default.Chat, contentDescription = "Chat with support")
            }
        }
    }
}
