package com.entab.phdwidget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject

/** Configuration for a single PHD widget instance. Only [schoolCode] is normally required. */
data class PhdWidgetConfig(
    val schoolCode: String,
    val host: String = "https://genie.globalmetaldirect.com",
    val extraConfig: Map<String, String> = emptyMap(),
    val handleBackNavigation: Boolean = true,
)

/** Lifecycle events surfaced by the widget. Always handle [Failed] — without it a load
 *  failure leaves the user on a blank view with no explanation. */
sealed class PhdWidgetEvent {
    data object Ready : PhdWidgetEvent()
    data object Close : PhdWidgetEvent()
    data class Failed(val description: String) : PhdWidgetEvent()
    data class Message(val payload: String) : PhdWidgetEvent()
}

/**
 * Plain-View host for the PHD widget. Always size this MATCH_PARENT — the widget's own CSS
 * resolves viewport units against whatever height the WebView is measured at, and a
 * zero-height parent collapses the panel permanently (reload will not recover it).
 *
 * Mount this on demand (add/remove from the view hierarchy) rather than leaving it attached
 * for the lifetime of a screen: the hosted WebView is a full-bounds transparent layer and
 * will intercept touches everywhere within its bounds, not just where the widget's own
 * button/panel are visually drawn.
 */
class PhdWidgetView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private var webView: WebView? = null
    var onEvent: ((PhdWidgetEvent) -> Unit)? = null

    @SuppressLint("SetJavaScriptEnabled")
    fun load(config: PhdWidgetConfig) {
        release()

        val wv = WebView(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    onEvent?.invoke(PhdWidgetEvent.Ready)
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?,
                ) {
                    if (request?.isForMainFrame != false) {
                        onEvent?.invoke(PhdWidgetEvent.Failed(error?.description?.toString() ?: "load failed"))
                    }
                }
            }
            loadDataWithBaseURL(config.host, buildHtml(config), "text/html", "utf-8", null)
        }

        webView = wv
        addView(wv, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    /** Tears down the WebView and its JS/network context. Call when the widget should close. */
    fun release() {
        webView?.let {
            removeView(it)
            it.destroy()
        }
        webView = null
    }

    override fun onDetachedFromWindow() {
        release()
        super.onDetachedFromWindow()
    }

    private fun buildHtml(config: PhdWidgetConfig): String {
        val cfgJson = JSONObject().apply {
            put("host", config.host)
            put("schoolCode", config.schoolCode)
            config.extraConfig.forEach { (k, v) -> put(k, v) }
        }.toString()

        return """
            <!doctype html>
            <html>
            <head><style>html,body{margin:0;padding:0;background:transparent}</style></head>
            <body>
              <script>
                window.phdWidgetConfig = $cfgJson;
                var s = document.createElement('script');
                s.src = '${config.host}/phd-widget/inject.js';
                s.async = true;
                document.body.appendChild(s);
              </script>
            </body>
            </html>
        """.trimIndent()
    }
}

/**
 * Jetpack Compose entry point. Mounts a [PhdWidgetView] for as long as this composable is
 * present in the tree — callers control on-demand mounting by conditionally including this
 * composable, not by hiding/showing it internally.
 */
@Composable
fun PhdWidget(
    config: PhdWidgetConfig,
    modifier: Modifier = Modifier,
    onEvent: (PhdWidgetEvent) -> Unit = {},
) {
    val context = LocalContext.current
    val view = remember(context) { PhdWidgetView(context) }

    if (config.handleBackNavigation) {
        BackHandler { onEvent(PhdWidgetEvent.Close) }
    }

    DisposableEffect(config) {
        view.onEvent = onEvent
        view.load(config)
        onDispose { view.release() }
    }

    AndroidView(
        factory = { view.apply { layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT) } },
        modifier = modifier,
    )
}
