package models;

import java.util.List;
import java.util.Map;

public class HttpResponseData {
    private int statusCode;
    private Map<String, List<String>> headers;
    private String body;

    public HttpResponseData(int statusCode, Map<String,List<String>> responseHeaders, String body) {
        this.statusCode = statusCode;
        this.headers = responseHeaders;
        this.body = body;
    }

    public int getStatusCode() {
        return this.statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public Map<String, List<String>> getHeaders() {
        return this.headers;
    }

    public void setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }

    public String getBody() {
        return this.body;
    }

    public void setBody(String body) {
        this.body = body;
    }
    
}
