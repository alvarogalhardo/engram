package com.alvarogalhardo.engram.ui.components

object HtmlTemplate {
    const val BASE_URL = "https://appassets.androidapp.com/media/"

    /** Embrulha o HTML da carta com CSS theme-aware (cores vêm do MaterialTheme). */
    fun wrap(body: String, textColor: String, codeBg: String, outline: String, primary: String): String = """
        <!doctype html>
        <html>
        <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <style>
            body {
                font-family: sans-serif;
                font-size: 18px;
                line-height: 1.5;
                color: $textColor;
                background: transparent;
                margin: 14px;
                word-wrap: break-word;
            }
            img { max-width: 100%; height: auto; border-radius: 8px; }
            pre, code {
                font-family: monospace;
                font-size: 15px;
                background: $codeBg;
                border-radius: 6px;
            }
            pre { padding: 12px; overflow-x: auto; }
            code { padding: 1px 5px; }
            pre code { padding: 0; background: transparent; }
            hr { border: none; border-top: 1px solid $outline; margin: 16px 0; }
            table { border-collapse: collapse; max-width: 100%; }
            th, td { border: 1px solid $outline; padding: 4px 10px; }
            a { color: $primary; }
            blockquote { border-left: 3px solid $outline; margin-left: 0; padding-left: 12px; }
        </style>
        </head>
        <body>$body</body>
        </html>
    """.trimIndent()
}
