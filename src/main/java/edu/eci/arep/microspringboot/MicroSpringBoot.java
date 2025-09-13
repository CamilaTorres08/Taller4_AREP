/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

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
                35000,
                Math.max(4, Runtime.getRuntime().availableProcessors() * 2),
                200,
                "/resources",
                "edu.eci.arep.microspringboot"
                );
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        Thread t = server.startAsync();
        t.join();
    }
}
