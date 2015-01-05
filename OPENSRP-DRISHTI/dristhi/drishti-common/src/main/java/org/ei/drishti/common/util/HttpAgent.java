package org.ei.drishti.common.util;

import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.HTTP;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;

@Component
public class HttpAgent {

    private final DefaultHttpClient httpClient;

    public HttpAgent() {
        BasicHttpParams basicHttpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(basicHttpParams, 30000);
        HttpConnectionParams.setSoTimeout(basicHttpParams, 60000);

        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        registry.register(new Scheme("https", sslSocketFactoryWithDrishtiCertificate(), 443));

        ClientConnectionManager connectionManager = new ThreadSafeClientConnManager(basicHttpParams, registry);
        httpClient = new DefaultHttpClient(connectionManager, basicHttpParams);
    }

    public HttpResponse post(String url, String data, String contentType) {
        HttpPost request = new HttpPost(url);
        try {
            request.setHeader(HTTP.CONTENT_TYPE, contentType);
            StringEntity entity = new StringEntity(data);
            entity.setContentEncoding(contentType);
            request.setEntity(entity);
            org.apache.http.HttpResponse response = httpClient.execute(request);
            return new HttpResponse(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK, IOUtils.toString(response.getEntity().getContent()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public HttpResponse get(String url, String username, String password) {
    	BasicHttpParams basicHttpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(basicHttpParams, 30000);
        HttpConnectionParams.setSoTimeout(basicHttpParams, 60000);

        DefaultHttpClient hc = new DefaultHttpClient(basicHttpParams);
        
        HttpGet request = new HttpGet(url);
        byte[] auth = Base64.encodeBase64((username+":"+password).getBytes());
        
        System.out.println(url + " :: " + new String(auth));
        request.addHeader ("Authorization", "Basic " + new String(auth));
        try {
            org.apache.http.HttpResponse response = hc.execute(request);
            return new HttpResponse(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK, IOUtils.toString(response.getEntity().getContent()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public HttpResponse post(String url, String payload, String data, String contentParamName) {
        try {
            String charset = "UTF-8";;

            if(url.endsWith("/")){
            	url = url.substring(0, url.lastIndexOf("/"));
            }
            
			URL urlo = new URL(url+"?"+payload);
			java.net.HttpURLConnection conn = (HttpURLConnection) urlo.openConnection();
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			conn.setDoInput(true);
			
			String boundary = Long.toHexString(System.currentTimeMillis()); // Just generate some unique random value.
	        String CRLF = "\r\n"; // Line separator required by multipart/form-data.

            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
			conn.setRequestProperty("Accept-Charset", charset);

            //con.setFixedLengthStreamingMode(request.toString().length());
			//con.addRequestProperty("Referer", "http://blog.dahanne.net");
			// Start the query
//			conn.connect();
			//OutputStream output = con.getOutputStream();
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(conn.getOutputStream(), charset), true); // true = autoFlush, important!

			// Send normal param.
		    writer.append("--" + boundary).append(CRLF);
		    writer.append("Content-Disposition: form-data; name=\""+contentParamName+"\"").append(CRLF);
		    writer.append("Content-Type: text/plain; charset=" + charset ).append(CRLF);
		    writer.append(CRLF);
		    System.out.println(data);
		    writer.append(data).append(CRLF).flush();
		    // End of multipart/form-data.
		    writer.append("--" + boundary + "--").append(CRLF);
		    if (writer != null) writer.close();

		    //if(output != null) output.close(); 
		    	
            return new HttpResponse(conn.getResponseCode() == HttpStatus.SC_OK, IOUtils.toString(conn.getInputStream()));
			
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public HttpResponse put(String url, Map<String, String> formParams) {
        HttpPut request = new HttpPut(url);
        try {
            List<NameValuePair> urlParameters = new ArrayList<>();
            for (String param : formParams.keySet()) {
                urlParameters.add(new BasicNameValuePair(param, formParams.get(param)));
            }
            request.setEntity(new UrlEncodedFormEntity(urlParameters));
            org.apache.http.HttpResponse response = httpClient.execute(request);
            return new HttpResponse(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK,
                    IOUtils.toString(response.getEntity().getContent()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public HttpResponse get(String url) {
        HttpGet request = new HttpGet(url);
        try {
            org.apache.http.HttpResponse response = httpClient.execute(request);
            return new HttpResponse(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK, IOUtils.toString(response.getEntity().getContent()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private SocketFactory sslSocketFactoryWithDrishtiCertificate() {
        try {
            Security.addProvider(new BouncyCastleProvider());
            KeyStore trustedKeystore = KeyStore.getInstance("BKS");
            InputStream inputStream = this.getClass().getResourceAsStream("/drishti_client.keystore");
            try {
                trustedKeystore.load(inputStream, "phone red pen".toCharArray());
            } finally {
                inputStream.close();
            }

            SSLSocketFactory socketFactory = new SSLSocketFactory(trustedKeystore);
            final X509HostnameVerifier oldVerifier = SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
            socketFactory.setHostnameVerifier(oldVerifier);
            return socketFactory;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
