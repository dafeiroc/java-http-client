import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

public class Main {
    public static void main(String[] args) {
        if (args.length != 8) {
            System.err.println("args.length should be 8.");
            System.exit(1);
        }

        String url = args[0];
        int connectTimeout = Integer.parseInt(args[1]);
        int readTimeout = Integer.parseInt(args[2]);
        int threadNum = Integer.parseInt(args[3]);
        long sleepTime = Long.parseLong(args[4]);
        int reqNum = Integer.parseInt(args[5]);
        int paramLength = Integer.parseInt(args[6]);
        int paramNum = Integer.parseInt(args[7]);

        Thread[] workers = new Thread[threadNum];

        for (int i = 0; i < threadNum; i++) {
            workers[i] = new Thread(new Worker(url, connectTimeout, readTimeout, sleepTime, reqNum, paramLength, paramNum));
            workers[i].start();
        }

        for (int i = 0; i < threadNum; i++) {
            try {
                workers[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

class Worker implements Runnable {
    private String url;
    private int connectTimeout;
    private int readTimeout;
    private long sleepTime;
    private int reqNum;
    private int paramLength;
    private String paramFormat;
    private int paramNum;

    Worker(String url, int connectTimeout, int readTimeout, long sleepTime, int reqNum, int paramLength, int paramNum) {
        this.url = url;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.sleepTime = sleepTime;
        this.reqNum = reqNum;
        this.paramLength = paramLength;
        this.paramFormat = "%0" + String.valueOf(paramLength) + "d";
        this.paramNum = paramNum;
    }

    private String createParams() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paramNum; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(String.format(paramFormat, (int)(Math.random() * (Math.pow(10, Math.min(paramLength, 6))))));
        }
        return sb.toString();
    }

    private HttpsURLConnection createConnection() throws KeyManagementException, NoSuchAlgorithmException, IOException {
        TrustManager[] tms = new TrustManager[]{new DummyX509TrustManager()};
        SSLContext sslcontext = SSLContext.getInstance("SSL");
        sslcontext.init(null, tms, null);

        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });

        URL connectUrl = new URL(this.url );
        HttpsURLConnection urlconn = (HttpsURLConnection) connectUrl.openConnection();
        urlconn.setConnectTimeout(this.connectTimeout);
        urlconn.setReadTimeout(this.readTimeout);
        urlconn.setRequestMethod("GET");
        urlconn.setSSLSocketFactory(sslcontext.getSocketFactory());

        return urlconn;
    }

    private void read(HttpsURLConnection urlconn) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(urlconn.getInputStream(), "utf8"));
        String line;
        StringBuilder sb = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
    }

    private void printLog(HttpsURLConnection urlcon, long connectElapsed, long readElapsed, Exception connectEx, Exception readEx) {
        System.out.println(urlcon.getURL().toString() +","+ new Date() + "," + connectElapsed + "," + readElapsed + "," +
                           (connectEx == null ? "" : connectEx) + "," + (readEx == null ? "" : readEx));
    }

    @Override
    public void run() {
        for (int i = 0; i < this.reqNum; i++) {
            try {
                Thread.sleep((long)((Math.random() + 0.5) * this.sleepTime));
            } catch (InterruptedException e) {
                e.printStackTrace();
                continue;
            }

            HttpsURLConnection urlconn = null;

            try {
                urlconn = createConnection();
           } catch (Exception e) {
                e.printStackTrace();
                continue;
            }

            Exception ex = null;

            long connectStart = System.currentTimeMillis();

            try {
                urlconn.connect();
            } catch (Exception e) {
                ex = e;
            }

            long connectElapsed = (System.currentTimeMillis() - connectStart);

            if (ex != null) {
                printLog(urlconn, connectElapsed, -1, ex, null);
                continue;
            }

            long readStart = System.currentTimeMillis();

            try {
                read(urlconn);
            } catch (Exception e) {
                ex = e;
            }

            long readElapsed = (System.currentTimeMillis() - readStart);

            printLog(urlconn, connectElapsed, readElapsed, null, ex);
        }
    }
}

class DummyX509TrustManager implements X509TrustManager {
    @Override
    public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
    }

    @Override
    public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }
}
