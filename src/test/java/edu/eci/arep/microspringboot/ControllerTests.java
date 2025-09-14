package edu.eci.arep.microspringboot;

import edu.eci.arep.microspringboot.annotations.GetMapping;
import edu.eci.arep.microspringboot.annotations.RequestMapping;
import edu.eci.arep.microspringboot.annotations.RestController;
import edu.eci.arep.microspringboot.connection.URLConnection;
import edu.eci.arep.microspringboot.examples.GreetingController;
import edu.eci.arep.microspringboot.httpserver.HttpServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;

import static edu.eci.arep.microspringboot.httpserver.HttpServer.staticfiles;
import static org.junit.Assert.*;

public class ControllerTests {
    private static Thread serverThread;
    private static final int port = 35002;
    static URLConnection urlConnection;
    static HttpServer server;
    @BeforeClass
    public static void setUp() throws Exception {
        urlConnection = new URLConnection(port);
        server = new HttpServer(
                port,
                Math.max(4, Runtime.getRuntime().availableProcessors() * 2),
                200,
                "static",
                "edu.eci.arep.microspringboot"
        );
        serverThread = server.startAsync();
        waitForServerToStart(10000);

    }
    @AfterClass
    public static void tearDown() throws InterruptedException {
        if (server != null) {
            server.stop();
        }
        if(serverThread != null) {
            serverThread.join();
        }
    }
    private static void waitForServerToStart(int timeoutMs) throws Exception {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("localhost", port), 1000);
                return; // Connection successful
            } catch (IOException e) {
                Thread.sleep(100);
            }
        }
        throw new RuntimeException("Server failed to start within " + timeoutMs + "ms");
    }
    /*
    *Not send parameter to endpoint /greeting, should response "hello world"
     */
    @Test
    public void testGreetingWithDefaultParameter() throws Exception {
        HttpURLConnection getConnection = urlConnection.createGetConnection("/app/greeting");
        String response = urlConnection.readResponse(getConnection);
        assertEquals("Hello World", response.trim());
        getConnection.disconnect();
    }
    /*
     *Sending parameter to endpoint /greeting, Should response the message with the parameter
     */
    @Test
    public void testGreetingWithCustomName() throws Exception {
        HttpURLConnection getConnection = urlConnection.createGetConnection("/app/greeting?name=Juan");
        String response = urlConnection.readResponse(getConnection);
        assertEquals("Hello Juan", response.trim());
        getConnection.disconnect();
    }

    /*
     *Testing endpoint /void, should return always "hello world"
     */
    @Test
    public void testVoidEndpoint() throws Exception {
        HttpURLConnection getConnection = urlConnection.createGetConnection("/app/void");
        String response = urlConnection.readResponse(getConnection);
        assertEquals("Hello World!", response.trim());
        getConnection.disconnect();
    }
    /*
    *Testing if default values are setting when we call /params
     */
    @Test
    public void testParamsWithDefaultValues() throws Exception {
        HttpURLConnection getConnection = urlConnection.createGetConnection("/app/params");
        String response = urlConnection.readResponse(getConnection);
        assertEquals("Name: Camila Gender: female Age: 23", response.trim());
        getConnection.disconnect();
    }
    /*
     *Testing if default 'Gender' and 'Age' values are setting when we call /params
     */
    @Test
    public void testParamsWithCustomName() throws Exception {
        HttpURLConnection getConnection = urlConnection.createGetConnection("/app/params?name=Carlos");
        String response = urlConnection.readResponse(getConnection);
        assertEquals("Name: Carlos Gender: female Age: 23", response.trim());
        getConnection.disconnect();
    }
    /*
     *Testing if default 'Name' and 'Gender' values are setting when we call /params
     */
    @Test
    public void testParamsWithCustomAge() throws Exception {
        HttpURLConnection getConnection = urlConnection.createGetConnection("/app/params?age=30");
        String response = urlConnection.readResponse(getConnection);
        assertEquals("Name: Camila Gender: female Age: 30", response.trim());
        getConnection.disconnect();
    }
    /*
     *Testing if default 'Age' value are setting when we call /params
     */
    @Test
    public void testParamsWithBothParameters() throws Exception {
        HttpURLConnection getConnection = urlConnection.createGetConnection("/app/params?name=Andres&gender=male");
        String response = urlConnection.readResponse(getConnection);
        assertEquals("Name: Andres Gender: male Age: 23", response.trim());
        getConnection.disconnect();
    }
    /*
     *Testing when we not send body message, should response with 204
     */
    @Test
    public void testBodyEndpointReturnsNoContent() throws Exception {
        HttpURLConnection getConnection = urlConnection.createGetConnection("/app/body");
        String response = urlConnection.readResponse(getConnection);
        int responseCode = getConnection.getResponseCode();
        assertEquals("Should return 204 No Content",204, responseCode);
        assertTrue("Response should be empty",
                response == null || response.trim().isEmpty());
        getConnection.disconnect();
    }

    /*
     *Testing if GreetingController has the RestController annotation
     */
    @Test
    public void testControllerHasRestControllerAnnotation() {
        Class<?> controllerClass = GreetingController.class;
        boolean hasRestController = controllerClass.isAnnotationPresent(RestController.class);
        assertTrue("Controller should have @RestController annotation", hasRestController);
    }
    /*
     *Testing if GreetingController has the RequestMapping annotation
     */
    @Test
    public void testControllerHasNotRequestMappingAnnotation() {
        Class<?> controllerClass = GreetingController.class;
        boolean hasRequestMapping = controllerClass.isAnnotationPresent(RequestMapping.class);
        assertFalse("Controller should have @RequestMapping annotation", hasRequestMapping);
    }
    /*
     *Testing if GreetingController has GetMapping annotation
     */
    @Test
    public void testGreetingMethodHasGetMappingAnnotation() throws NoSuchMethodException {
        Method greetingMethod = GreetingController.class.getMethod("greeting", String.class);
        boolean hasGetMapping = greetingMethod.isAnnotationPresent(GetMapping.class);
        assertTrue("greeting method should have @GetMapping annotation", hasGetMapping);

        GetMapping mapping = greetingMethod.getAnnotation(GetMapping.class);
        assertEquals("Should map to /greeting", "/greeting", mapping.value());
    }

}
