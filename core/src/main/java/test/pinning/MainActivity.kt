package test.pinning

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.datatheorem.android.trustkit.TrustKit
import com.datatheorem.android.trustkit.pinning.OkHttp3Helper
import com.datatheorem.android.trustkit.reporting.BackgroundReporter
import com.datatheorem.android.trustkit.reporting.PinningFailureReport
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
            val receiver = PinningFailureReportBroadcastReceiver()
            LocalBroadcastManager.getInstance(this)
                .registerReceiver(
                    receiver,
                    IntentFilter(BackgroundReporter.REPORT_VALIDATION_EVENT)
                )
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

    private inner class PinningFailureReportBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent) {
            val report =
                intent.getSerializableExtra(BackgroundReporter.EXTRA_REPORT) as PinningFailureReport?
            updateUi("REPORT = $report")
        }
    }
}
