package edu.eci.arep.microspringboot.connection;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class URLConnection {
    int port;
    public URLConnection(int port) {
        this.port = port;
    }

    public HttpURLConnection createPostConnection(String path, String jsonPayload) throws Exception {
        HttpURLConnection connection = createConnection(path, "POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Content-Length", String.valueOf(jsonPayload.length()));

        try (OutputStream out = connection.getOutputStream()) {
            out.write(jsonPayload.getBytes("UTF-8"));
            out.flush();
        }
        return connection;
    }

    public HttpURLConnection createGetConnection(String path) throws Exception {
        return createConnection(path, "GET");
    }

    public HttpURLConnection createConnection(String path, String method) throws Exception {
        URL url = new URL("http://localhost:" + port + path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setDoOutput(true);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        return connection;
    }

    public String readResponse(HttpURLConnection connection) throws Exception {
        InputStream inputStream;
        try {
            inputStream = connection.getInputStream();
        } catch (IOException e) {
            inputStream = connection.getErrorStream();
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }
}
