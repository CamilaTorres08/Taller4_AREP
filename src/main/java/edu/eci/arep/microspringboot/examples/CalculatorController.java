package edu.eci.arep.microspringboot.examples;

import edu.eci.arep.microspringboot.annotations.GetMapping;
import edu.eci.arep.microspringboot.annotations.RequestMapping;
import edu.eci.arep.microspringboot.annotations.RequestParam;
import edu.eci.arep.microspringboot.annotations.RestController;

@RestController
@RequestMapping("/v1/calculate/maths")
public class CalculatorController {

    @GetMapping
    public static String calculate(@RequestParam(value = "operation", defaultValue = "+") String operation, @RequestParam(value = "a", defaultValue = "1") String a, @RequestParam(value = "b", defaultValue = "1") String b){
        int numbera = Integer.parseInt(a);
        int numberb = Integer.parseInt(b);
        String result = "Result: ";
        return switch (operation) {
            case "+" -> result + (numbera + numberb);
            case "-" -> result + (numbera - numberb);
            case "*" -> result + (numbera * numberb);
            default -> result + (numberb > 0 ? numbera / numberb : "Cannot divide");
        };
    }
    @GetMapping("/square")
    public static String getSquare(@RequestParam(value = "number", defaultValue = "1") String number){
        return "Square of "+number+" : "+Integer.parseInt(number)*Integer.parseInt(number);
    }
}
