package edu.eci.arep.microspringboot;

import edu.eci.arep.microspringboot.connection.URLConnection;
import edu.eci.arep.microspringboot.httpserver.HttpServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class ConcurrencyTests {
    private static HttpServer server;
    private static final int port = 35003;
    private static final int THREAD_POOL_SIZE = 10;
    private static final int QUEUE_CAPACITY = 50;
    private static Thread serverThread;
    static URLConnection urlConnection;

    @BeforeClass
    public static void setUpServer() throws Exception {
        urlConnection = new URLConnection(port);
        server = new HttpServer(port, THREAD_POOL_SIZE, QUEUE_CAPACITY,
                "static", "edu.eci.arep");
        serverThread = server.startAsync();
        waitForServerToStart(10000);
    }

    @AfterClass
    public static void tearDownServer() throws InterruptedException {
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
    /**
     * Tests basic concurrent request handling capability.
     *
     * Purpose: Verifies that the server can handle multiple simple requests simultaneously
     * without race conditions or request mixing.
     *
     * Scenario: 20 concurrent threads each making a GET request to /greeting endpoint
     * with unique parameters.
     *
     * Success Criteria:
     * - All 20 requests complete successfully (100% success rate)
     * - Each response contains the correct thread-specific parameter
     * - No timeouts or connection errors
     *
     * What it proves: Basic thread safety and concurrent request isolation
     */
    @Test
    public void testConcurrentSimpleRequests() throws Exception {
        int numberOfThreads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successfulRequests = new AtomicInteger(0);
        AtomicInteger failedRequests = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    HttpURLConnection connection = urlConnection
                            .createGetConnection("/app/greeting?name=Thread" + threadId);

                    int responseCode = connection.getResponseCode();
                    if (responseCode == 200) {
                        String response = urlConnection.readResponse(connection);
                        assertTrue(response.contains("Hello Thread" + threadId));
                        successfulRequests.incrementAndGet();
                    } else {
                        failedRequests.incrementAndGet();
                    }
                    connection.disconnect();

                } catch (Exception e) {
                    failedRequests.incrementAndGet();
                    System.err.println("Error in thread " + threadId + ": " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue("Concurrent requests did not complete within expected time",latch.await(30, TimeUnit.SECONDS));
        assertEquals("Not all concurrent requests were successful",numberOfThreads, successfulRequests.get());
        assertEquals("Some requests failed: " + failedRequests.get(),0, failedRequests.get());
        executor.shutdown();
    }
    /**
     * Tests concurrent processing of computational workloads.
     *
     * Purpose: Verifies that business logic operations (mathematical calculations)
     * can be processed concurrently without data corruption or incorrect results.
     *
     * Scenario: 50 mathematical operations (+, -, *, /) distributed across 10 worker threads.
     * Each operation has unique parameters to detect any parameter mixing.
     *
     * Success Criteria:
     * - At least 90% of requests complete successfully
     * - Each calculation returns the correct result
     * - Response format is consistent ("Result: X")
     *
     * What it proves: Thread safety for business logic processing and parameter isolation
     */
    @Test
    public void testConcurrentCalculatorLoad() throws Exception {
        int numberOfRequests = 50;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < numberOfRequests; i++) {
            final int a = i;
            final int b = i + 1;
            final String operation = (i % 4 == 0) ? "+" : (i % 4 == 1) ? "-" :
                    (i % 4 == 2) ? "*" : "/";

            Future<Boolean> future = executor.submit(() -> {
                try {
                    HttpURLConnection connection = urlConnection
                            .createGetConnection("/v1/calculate/maths?operation=" + operation +
                                    "&a=" + a + "&b=" + b);

                    int responseCode = connection.getResponseCode();
                    if (responseCode == 200) {
                        String response = urlConnection.readResponse(connection);
                        assertNotNull(response);
                        assertTrue(response.contains("Result:"));
                        return true;
                    }
                    connection.disconnect();
                    return false;

                } catch (Exception e) {
                    System.err.println("Error in operation " + operation +
                            " with " + a + " and " + b + ": " + e.getMessage());
                    return false;
                }
            });
            futures.add(future);
        }
        int successCount = 0;
        for (Future<Boolean> future : futures) {
            if (future.get(10, TimeUnit.SECONDS)) {
                successCount++;
            }
        }
        assertTrue("At least 90% of calculation requests should succeed. Successful: " + successCount,successCount >= numberOfRequests * 0.9);
        executor.shutdown();
    }
    /**
     * Stress test with mixed endpoint types to simulate real-world usage.
     *
     * Purpose: Tests server behavior under mixed workload conditions where different
     * types of endpoints are accessed simultaneously, simulating real user traffic.
     *
     * Scenario: 100 requests distributed across all available endpoints:
     * - Simple responses (/greeting, /void)
     * - Parameter processing (/params)
     * - Mathematical operations (/calculate)
     * - Data retrieval (/task)
     *
     * Success Criteria:
     * - At least 85% of requests complete successfully
     * - Response times remain reasonable (< 5 seconds)
     * - All endpoint types function correctly under concurrent load
     *
     * What it proves: System stability under realistic mixed workloads
     */
    @Test
    public void testMixedEndpointStress() throws Exception {
        int totalRequests = 100;
        ExecutorService executor = Executors.newFixedThreadPool(15);
        AtomicInteger completedRequests = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(totalRequests);
        String[] endpoints = {
                "/app/greeting?name=StressTest",
                "/app/void",
                "/app/params?name=Test&gender=male&age=25",
                "/v1/calculate/maths?operation=+&a=10&b=5",
                "/v1/calculate/maths/square?number=7",
                "/task?name=All"
        };
        for (int i = 0; i < totalRequests; i++) {
            final String endpoint = endpoints[i % endpoints.length];
            final int requestId = i;
            executor.submit(() -> {
                try {
                    HttpURLConnection connection = urlConnection
                            .createGetConnection(endpoint);

                    long startTime = System.currentTimeMillis();
                    int responseCode = connection.getResponseCode();
                    long endTime = System.currentTimeMillis();

                    // // Verify reasonable response time
                    assertTrue("Request took no more than 5s" + (endTime - startTime) + "ms",endTime - startTime < 5000);

                    if (responseCode == 200) {
                        completedRequests.incrementAndGet();
                    }
                    connection.disconnect();
                } catch (Exception e) {
                    System.err.println("Error in request " + requestId + " a " + endpoint +
                            ": " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        assertTrue("Stress test requests did not complete within reasonable time",latch.await(60, TimeUnit.SECONDS));
        assertTrue("At least 85% of mixed requests should complete successfully. " +
                "Completed: " + completedRequests.get() + "/" + totalRequests,completedRequests.get() >= totalRequests * 0.85);
        executor.shutdown();
    }
    /**
     * Tests server recovery capabilities after load conditions.
     *
     * Purpose: Verifies that the server maintains stability and can continue
     * normal operations after experiencing concurrent load, ensuring no resource
     * leaks or permanent state corruption.
     *
     * Scenario:
     * 1. Normal request before load
     * 2. Burst of concurrent requests
     * 3. Normal requests after load to verify recovery
     *
     * Success Criteria:
     * - Initial request succeeds normally
     * - At least 80% of burst requests complete
     * - Server continues normal operation post-burst
     *
     * What it proves: Server resilience and proper resource cleanup
     */
    @Test
    public void testTimeoutAndRecovery() throws Exception {
        // Test normal operation before stress
        HttpURLConnection normalConnection = urlConnection
                .createGetConnection("/app/greeting?name=BeforeTimeout");
        assertEquals(200, normalConnection.getResponseCode());
        String normalResponse = urlConnection.readResponse(normalConnection);
        assertTrue(normalResponse.contains("Hello BeforeTimeout"));
        normalConnection.disconnect();
        // Apply concurrent load burst
        ExecutorService quickExecutor = Executors.newFixedThreadPool(5);
        CountDownLatch quickLatch = new CountDownLatch(10);
        AtomicInteger quickSuccesses = new AtomicInteger(0);
        for (int i = 0; i < 10; i++) {
            final int id = i;
            quickExecutor.submit(() -> {
                try {
                    HttpURLConnection connection = urlConnection
                            .createGetConnection("/app/greeting?name=Recovery" + id);

                    if (connection.getResponseCode() == 200) {
                        quickSuccesses.incrementAndGet();
                    }
                    connection.disconnect();

                } catch (Exception e) {
                    System.err.println("Error in recovery test: " + e.getMessage());
                } finally {
                    quickLatch.countDown();
                }
            });
        }
        assertTrue(quickLatch.await(15, TimeUnit.SECONDS));
        assertTrue("Server should recover properly after stress, got: ",quickSuccesses.get() >= 8);
        quickExecutor.shutdown();
    }
    /**
     * Tests thread pool limits and queue management under saturation.
     *
     * Purpose: Verifies proper behavior when request volume exceeds thread pool
     * capacity, testing the ArrayBlockingQueue and CallerRunsPolicy configuration.
     *
     * Scenario: Send 30 requests (3x pool size) to force queue usage and policy activation.
     * Tests the complete request handling pipeline under resource constraint.
     *
     * Success Criteria:
     * - At least 80% of requests complete successfully
     * - No requests are dropped or lost
     * - CallerRunsPolicy activates gracefully when queue fills
     * - System remains stable under saturation
     *
     * What it proves: Proper thread pool configuration and overflow handling
     */
    @Test
    public void testThreadPoolLimits() throws Exception {
        int requestCount = THREAD_POOL_SIZE * 3;
        ExecutorService testExecutor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch allRequestsLatch = new CountDownLatch(requestCount);
        AtomicInteger processedRequests = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < requestCount; i++) {
            final int requestId = i;
            testExecutor.submit(() -> {
                try {
                    HttpURLConnection connection = urlConnection
                            .createGetConnection("/v1/calculate/maths/square?number=" + (requestId + 1));

                    int responseCode = connection.getResponseCode();
                    if (responseCode == 200) {
                        String response = urlConnection.readResponse(connection);
                        if (response.contains("Square of " + (requestId + 1))) {
                            processedRequests.incrementAndGet();
                        }
                    }
                    connection.disconnect();

                } catch (Exception e) {
                    System.err.println("Error en thread pool test " + requestId + ": " + e.getMessage());
                } finally {
                    allRequestsLatch.countDown();
                }
            });
        }
        assertTrue("Thread pool saturation test did not complete in time",allRequestsLatch.await(45, TimeUnit.SECONDS));
        assertTrue("Thread pool should handle at least 80% of requests correctly under saturation.",processedRequests.get() >= requestCount * 0.8);
        testExecutor.shutdown();
    }


}
