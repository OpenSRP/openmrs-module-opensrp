package org.ei.drishti.client;

import static org.apache.http.HttpStatus.SC_OK;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.ei.drishti.util.HttpResponseUtil;

public class GZipEncodingHttpClient {
    private DefaultHttpClient httpClient;

    public GZipEncodingHttpClient(DefaultHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public InputStream fetchContent(HttpGet request) throws IOException {
        if (!request.containsHeader("Accept-Encoding")) {
            request.addHeader("Accept-Encoding", "gzip");
        }

        HttpResponse response = httpClient.execute(request);
        if (response.getStatusLine().getStatusCode() != SC_OK) {
            throw new IOException("Invalid status code: " + response.getStatusLine().getStatusCode());
        }

        return HttpResponseUtil.getResponseStream(response);
    }

    public HttpResponse execute(HttpGet request) throws IOException {
        return httpClient.execute(request);
    }

    public CredentialsProvider getCredentialsProvider() {
        return httpClient.getCredentialsProvider();
    }

    public int postContent(HttpPost request) throws IOException {
        HttpResponse response = httpClient.execute(request);
        return response.getStatusLine().getStatusCode();
    }
}
