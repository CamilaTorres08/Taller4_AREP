package edu.eci.arep.microspringboot;

import edu.eci.arep.microspringboot.httpserver.HttpServer;

/**
 *
 * @author andrea.torres-g
 */
public class MicroSpringBoot {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Starting microspringboot");
        HttpServer server = new HttpServer(
                getPort(),
                Math.max(4, Runtime.getRuntime().availableProcessors() * 2),
                200,
                "static",
                "edu.eci.arep.microspringboot"
                );
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        Thread t = server.startAsync();
        t.join();
    }

    private static int getPort() {
        if (System.getenv("PORT") != null) {
            return Integer.parseInt(System.getenv("PORT"));
        }
        return 35000;
    }
}
