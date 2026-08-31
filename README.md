# PHD Android Widget

Native Android host for the Genie Parent Help Desk web widget. Ships as a small Gradle
library (`phdwidget`) that wraps the same `inject.js` widget used on the web in a `WebView`,
exposed as a Jetpack Compose composable and a plain `View`. No native reimplementation of the
chat flow — the web widget stays the single source of truth.

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
    implementation("com.github.<org>:phd-android-sdk:<version>")
}
```

## Usage

```kotlin
@Composable
fun SupportScreen(schoolCode: String) {
    PhdWidget(
        config = PhdWidgetConfig(schoolCode = schoolCode),
        modifier = Modifier.fillMaxSize(),
    )
}
```

See [`sample/`](sample) for a full example, including the on-demand mount/unmount pattern
required when overlaying the widget on an existing screen (see **Touch handling** below).

`PhdWidgetConfig.autoOpen` defaults to `true`: since mounting this composable is already the
user's "open chat" tap, the panel opens immediately instead of requiring a second tap on the
widget's own internal fab. Pass `autoOpen = false` to keep the widget's admin-configured
default instead (e.g. a persistent, always-mounted embed).

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
| `Close`   | User dismissed the panel, or Back was pressed with no widget history left |
| `Failed`  | Script could not load — no network, TLS rejected, wrong host, or host down |
| `Message` | Widget posted a message over the JS bridge |

Always handle `Failed` — without it a load failure leaves the user on a blank view.

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
