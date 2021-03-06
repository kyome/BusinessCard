package com.jaehyun.businesscard.network.ssl;

import android.util.Log;

import com.jaehyun.businesscard.BusinessCardApplication;
import com.jaehyun.businesscard.R;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;

public class SelfSigningHelper {
    private SSLContext sslContext;
    private TrustManagerFactory tmf;

    private SelfSigningHelper() {
        setUp();
    }

    private static class SelfSigningClientBuilderHolder{

        public static final SelfSigningHelper INSTANCE = new SelfSigningHelper();
    }

    public static SelfSigningHelper getInstance() {
        return SelfSigningClientBuilderHolder.INSTANCE;
    }

    public void setUp() {

        CertificateFactory cf;
        Certificate ca;

        InputStream caInput;
        try {
            cf = CertificateFactory.getInstance("X.509");
            caInput = BusinessCardApplication.getAppContext().getResources().openRawResource(R.raw.my_cert);

            ca = cf.generateCertificate(caInput);
            System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());

            // Create a KeyStore containing our trusted CAs
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null,null);
            keyStore.setCertificateEntry("ca", ca);

            // Create a TrustManager that trusts the CAs in our KeyStore
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);


            // Create an SSLContext that uses our TrustManager
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), new java.security.SecureRandom());

            caInput.close();
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException | KeyManagementException e) {
            e.printStackTrace();
        }
    }

    public OkHttpClient.Builder setSSLOkHttp(OkHttpClient.Builder builder,String target){

        builder.sslSocketFactory(getInstance().sslContext.getSocketFactory(), (X509TrustManager)getInstance().tmf.getTrustManagers()[0]);
        builder.hostnameVerifier((hostname, session) -> {
            if (hostname.contentEquals(target)) {
                Log.d("test", "Approving certificate for host " + hostname);
                return true;
            }else {
                Log.d("test", "fail " + hostname);
                return false;
            }
        });
        return builder;
    }

    public SSLContext getSslContext() {
        return sslContext;
    }

    public TrustManagerFactory getTmf() {
        return tmf;
    }
}