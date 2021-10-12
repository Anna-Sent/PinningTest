package test.pinning

import com.datatheorem.android.trustkit.TrustKit
import com.datatheorem.android.trustkit.pinning.OkHttp3Helper
import okhttp3.OkHttpClient
import javax.net.ssl.HttpsURLConnection

class MainActivity : BaseActivity() {

    private val useTrustKit by lazy {
        val id = resources.getIdentifier("use_trust_kit", "bool", packageName)
        resources.getBoolean(id)
    }

    override fun init() {
        if (useTrustKit) {
            TrustKit.initializeWithNetworkSecurityConfiguration(this)
        }
    }

    override fun HttpsURLConnection.fix() =
        if (useTrustKit) {
            apply {
                sslSocketFactory = TrustKit.getInstance().getSSLSocketFactory(url.host)
            }
        } else {
            this
        }

    override fun OkHttpClient.Builder.fix() =
        if (useTrustKit) {
            apply {
                sslSocketFactory(
                    OkHttp3Helper.getSSLSocketFactory(),
                    OkHttp3Helper.getTrustManager()
                )
                addInterceptor(OkHttp3Helper.getPinningInterceptor())
            }
        } else {
            this
        }
}
