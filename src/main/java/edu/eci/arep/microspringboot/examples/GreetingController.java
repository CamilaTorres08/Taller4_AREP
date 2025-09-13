/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.eci.arep.microspringboot.examples;

import edu.eci.arep.microspringboot.annotations.GetMapping;
import edu.eci.arep.microspringboot.annotations.RequestParam;
import edu.eci.arep.microspringboot.annotations.RestController;


/**
 *
 * @author andrea.torres-g
 */
@RestController
public class GreetingController {

	@GetMapping("/greeting")
	public static String greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
		return "Hello " + name;
	}
	@GetMapping("/void")
	public static String defaultValue() {
		return "Hello World!";
	}

	@GetMapping("/params")
	public static String params(@RequestParam(value = "name", defaultValue = "Camila") String name,@RequestParam(value = "gender", defaultValue = "female") String gender,@RequestParam(value = "age", defaultValue = "23") String age) {
		return "Name: " + name + " Gender: " + gender+ " Age: " + age;
	}

	@GetMapping("/body")
	public static void sendNoBody(@RequestParam(value = "name", defaultValue = "World") String name,@RequestParam(value = "gender", defaultValue = "female") String gender) {
		System.out.println("Hello " + name + " Gender: " + gender);
	}


}
