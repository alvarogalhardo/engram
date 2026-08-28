package com.alvarogalhardo.engram.ui.components

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import java.io.File

/**
 * Renderiza o HTML de uma carta num WebView (JS desligado, sem acesso a arquivos).
 * Imagens em filesDir/media são servidas via WebViewAssetLoader, então
 * `<img src="foo.png">` do Anki resolve direto pela BASE_URL relativa.
 */
@Composable
fun CardRenderer(html: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val page = remember(html, colors) {
        HtmlTemplate.wrap(
            body = html,
            textColor = colors.onSurface.toHex(),
            codeBg = colors.surfaceVariant.toHex(),
            outline = colors.outlineVariant.toHex(),
            primary = colors.primary.toHex(),
        )
    }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val assetLoader = WebViewAssetLoader.Builder()
                .setDomain("appassets.androidapp.com")
                .addPathHandler(
                    "/media/",
                    WebViewAssetLoader.InternalStoragePathHandler(ctx, File(ctx.filesDir, "media")),
                )
                .build()
            WebView(ctx).apply {
                settings.javaScriptEnabled = false
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest,
                    ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)

                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest,
                    ): Boolean = true // bloqueia navegação para fora da carta
                }
            }
        },
        update = { webView ->
            if (webView.tag != page) {
                webView.tag = page
                webView.loadDataWithBaseURL(HtmlTemplate.BASE_URL, page, "text/html", "utf-8", null)
            }
        },
    )
}

private fun Color.toHex(): String = "#%06X".format(0xFFFFFF and toArgb())
