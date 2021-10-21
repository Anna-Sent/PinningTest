package test.pinning

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.webkit.*
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import javax.net.ssl.HttpsURLConnection

open class BaseActivity : AppCompatActivity(R.layout.activity_main), CoroutineScope by MainScope() {

    companion object {

        private const val IMAGE_URL =
            "https://i.picsum.photos/id/995/536/354.jpg?hmac=kARkIcQD-5FYzmRwd89uPn6yxoJvaCg43bkO-kABGGE"
        private const val SITE_URL = "https://65apps.com"
        private const val ANY_URL = IMAGE_URL
    }

    private val webView by lazy { findViewById<WebView>(R.id.web_view) }
    private val image by lazy { findViewById<ImageView>(R.id.image) }
    private val text by lazy { findViewById<TextView>(R.id.text) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.app_name)

        text.movementMethod = ScrollingMovementMethod()

        init()

        testGlide()

        testWebView()

        testJavaApi()

        testOkHttp()
    }

    private fun testGlide() {
        Glide.with(this)
            .load(IMAGE_URL)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .listener(
                object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        updateUi("Glide: failed with ${e?.logRootCauses()}")
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }

                    private fun GlideException.logRootCauses() = buildString {
                        val causes = rootCauses
                        for ((i, cause) in causes.withIndex()) {
                            append("Root cause (${i + 1} of ${causes.size}) $cause ${cause.cause}")
                        }
                    }
                })
            .into(image)
    }

    private fun testWebView() {
        webView.webViewClient = object : WebViewClient() {

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                @Suppress("deprecation")
                super.onReceivedError(view, errorCode, description, failingUrl)
                updateUi("WebView: onReceivedError $errorCode $description")
            }

            @RequiresApi(Build.VERSION_CODES.M)
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                updateUi("WebView: onReceivedError ${error?.errorCode} ${error?.description}")
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                updateUi("WebView: onReceivedHttpError ${errorResponse?.reasonPhrase}")
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                super.onReceivedSslError(view, handler, error)
                updateUi("WebView: onReceivedSslError $error")
            }
        }
        webView.loadUrl(SITE_URL)
    }

    private fun testJavaApi() {
        doRequest("JavaApi") {
            val url = URL(ANY_URL)
            val connection = url.openConnection() as HttpsURLConnection
            connection.fix()
            BufferedReader(
                InputStreamReader(
                    connection.inputStream
                )
            ).use { it.readLines() }
        }
    }

    private fun testOkHttp() {
        doRequest("OkHttp") {
            val client = OkHttpClient.Builder()
                .fix()
                .followRedirects(false)
                .followSslRedirects(false)
                .build();
            val call = client.newCall(
                Request.Builder()
                    .url(ANY_URL)
                    .build()
            )
            call.execute()
        }
    }

    private fun doRequest(tag: String, block: () -> Unit) {
        launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    block()
                    "$tag: success"
                } catch (e: Exception) {
                    "$tag: failed with $e"
                }
            }

            updateUi(result)
        }
    }

    protected open fun init() {
        // no op
    }

    protected open fun HttpsURLConnection.fix() = this

    protected open fun OkHttpClient.Builder.fix() = this

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    @SuppressLint("SetTextI18n")
    protected fun updateUi(message: String) {
        text.text = text.text.toString() + "\n\n" + message
    }
}
