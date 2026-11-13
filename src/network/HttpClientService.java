package network;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import models.HttpRequestData;
import models.HttpResponseData;

public class    HttpClientService {
    private HttpClient client;

    public HttpClientService() {
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NORMAL).build();;
    }

    public HttpResponseData sendRequest(HttpRequestData requestData) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder().uri(URI.create(requestData.getUrl())).timeout(Duration.ofSeconds(30));
        
        String method = requestData.getMethod().toUpperCase();
        String body = requestData.getBody();
        Map<String, String> headers = requestData.getHeaders();

        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                request.header(header.getKey(), header.getValue());
            }
        }

        switch (method) {
            case "GET":
                request.GET();
                break;
            
            case "POST":
                request.POST(HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
                break;

            case "PUT":
                request.PUT(HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
                break;

            case "DELETE":
                request.DELETE();
                break;
        
            default:
                break;
        }

        HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        Map<String, List<String>> responseHeaders = response.headers().map();

        return new HttpResponseData(response.statusCode(), responseHeaders, response.body());
    }

}
