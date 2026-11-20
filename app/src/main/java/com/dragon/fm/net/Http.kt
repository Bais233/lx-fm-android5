package com.dragon.fm.net

import android.content.Context
import android.os.Build
import com.dragon.fm.R
import okhttp3.OkHttpClient
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

object Http {

    @Volatile
    private var cached: OkHttpClient? = null

    fun client(context: Context): OkHttpClient {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val builder = OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
            if (Build.VERSION.SDK_INT < 24) {
                addIsrgRoot(context, builder)
            }
            val client = builder.build()
            cached = client
            return client
        }
    }

    // ponytail: 只有 API<24 需要。5.x/6.x 的系统信任库里没有 ISRG Root X1（Let's Encrypt），
    // 而 networkSecurityConfig 要 API 24 才生效 —— 只能自己拼一个信任库。升到 7.1+ 后可整段删掉。
    private fun addIsrgRoot(context: Context, builder: OkHttpClient.Builder) {
        val store = KeyStore.getInstance(KeyStore.getDefaultType()).apply { load(null) }
        val system = KeyStore.getInstance("AndroidCAStore").apply { load(null) }
        val aliases = system.aliases()
        while (aliases.hasMoreElements()) {
            val alias = aliases.nextElement()
            (system.getCertificate(alias) as? X509Certificate)?.let { store.setCertificateEntry(alias, it) }
        }
        context.resources.openRawResource(R.raw.isrg_root_x1).use { input ->
            val cert = CertificateFactory.getInstance("X.509").generateCertificate(input) as X509Certificate
            store.setCertificateEntry("isrg-root-x1", cert)
        }
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply { init(store) }
        val trustManager = tmf.trustManagers.filterIsInstance<X509TrustManager>().first()
        val sslContext = SSLContext.getInstance("TLS").apply { init(null, tmf.trustManagers, null) }
        builder.sslSocketFactory(sslContext.socketFactory, trustManager)
    }
}
