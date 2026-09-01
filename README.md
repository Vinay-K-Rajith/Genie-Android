# Genie Android Chatbot SDK

Native Android host for the Genie Parent Help Desk chatbot. Ships as a small Gradle library
(`phdwidget`) with two integration shapes:

- **`PhdChatActivity`** — launch it and it owns its whole screen, including Back and closing,
  the same way Yellow.ai's own SDK launches a dedicated Activity. No lifecycle glue required.
  Use this unless you have a specific reason not to.
- **`PhdWidget`** — a Jetpack Compose composable (or `PhdWidgetView` for the plain `View`
  system) for overlaying chat inside an existing screen. You own mounting/unmounting and must
  react to `Close` yourself — see **Touch handling** and **Events** below.

## Install

Via [JitPack](https://jitpack.io):

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}
```

```kotlin
// app/build.gradle.kts
dependencies {
    implementation("com.github.Vinay-K-Rajith.Genie-Android:phdwidget:<version>")
}
```

`<version>` is a [release tag](https://github.com/Vinay-K-Rajith/Genie-Android/releases) of
this repo (or a commit hash, e.g. `e88f3de`, for an unreleased build). This is a multi-module
repo, so the group id is `com.github.<user>.<repo>` and `phdwidget` is the artifact — not
`com.github.<user>:<repo>` as for a single-module repo.

## Usage

Recommended — launch the chat as its own screen, same as `startActivity` for any other screen:

```kotlin
PhdChatActivity.start(context, PhdWidgetConfig(schoolCode = schoolCode))
```

Back, close, and load-failure retry are all handled internally. Nothing else to wire up.

Alternative — embed inline (e.g. a persistent support FAB over live content):

```kotlin
@Composable
fun SupportScreen(schoolCode: String) {
    PhdWidget(
        config = PhdWidgetConfig(schoolCode = schoolCode),
        modifier = Modifier.fillMaxSize(),
        // Required: PhdWidget does not unmount itself. If you don't stop rendering it here,
        // system Back will appear to do nothing while the widget is open — see Events below.
        onEvent = { if (it is PhdWidgetEvent.Close) navController.popBackStack() },
    )
}
```

See [`sample/`](sample) for a full example of the inline pattern, including the on-demand
mount/unmount required when overlaying the widget on an existing screen (see **Touch handling**
below).

Not using Compose? `PhdWidgetView` is the plain-`View` equivalent of `PhdWidget`. As its own
full-screen Activity, system Back finishes it for free (default Activity behavior); you still
need to call `finish()` yourself on `Close`, since the widget's own close button doesn't:

```kotlin
class SupportActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widget = PhdWidgetView(this)
        setContentView(widget)
        widget.onEvent = { if (it is PhdWidgetEvent.Close) finish() }
        widget.load(PhdWidgetConfig(schoolCode = schoolCode))
    }
}
```

`PhdWidgetConfig.autoOpen` defaults to `true`: since launching/mounting is already the user's
"open chat" tap, the panel opens immediately instead of requiring a second tap on the widget's
own internal fab. Pass `autoOpen = false` to keep the widget's admin-configured default instead
(e.g. a persistent, always-mounted embed).

## Requirements

- `minSdk 24`+
- System WebView present on the device
- `INTERNET` permission (declared by the library)

## Touch handling

The widget's `WebView` is a full-bounds transparent layer — it intercepts touches across its
entire area, not just where the floating button/panel are visually drawn. Never leave it
mounted permanently over a screen with other interactive content (e.g. a login form). Mount
it only while open, driven by your own state:

```kotlin
var showHelp by remember { mutableStateOf(false) }

Box(Modifier.fillMaxSize()) {
    YourScreen()
    if (showHelp) {
        PhdWidget(
            config = PhdWidgetConfig(schoolCode = schoolCode),
            modifier = Modifier.fillMaxSize(),
            onEvent = { if (it is PhdWidgetEvent.Close) showHelp = false },
        )
    } else {
        FloatingActionButton(onClick = { showHelp = true }) { /* ... */ }
    }
}
```

## Layout rules

Always size the widget `fillMaxSize()` / `MATCH_PARENT`. The panel resolves CSS viewport units
(`svh`) against the height it's measured at on first layout; a zero-height parent collapses it
permanently, and reloading will not recover it.

## Events

| Event     | Emitted when |
|-----------|--------------|
| `Ready`   | Script loaded, chat surface interactive |
| `Close`   | User tapped the panel's close button, or pressed system Back |
| `Failed`  | Script could not load — no network, TLS rejected, wrong host, or host down |
| `Message` | Widget posted a message over the JS bridge |

Always handle both `Close` and `Failed`. `Close` is not handled internally — Back is
implemented by emitting the event, not by unmounting the widget itself. If `onEvent` ignores
`Close`, Back will appear to do nothing while the widget is open. `Failed` unhandled leaves the
user on a blank view after a load failure.

## Release builds

If you minify, keep the JS bridge:

```proguard
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
```

(Already included as a consumer ProGuard rule — no action needed for a `debug`/default build.)

## License

MIT — see [LICENSE](LICENSE).
