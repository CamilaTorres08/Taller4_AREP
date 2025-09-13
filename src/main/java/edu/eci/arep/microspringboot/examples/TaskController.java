package edu.eci.arep.microspringboot.examples;

import edu.eci.arep.microspringboot.annotations.GetMapping;
import edu.eci.arep.microspringboot.annotations.RequestMapping;
import edu.eci.arep.microspringboot.annotations.RequestParam;
import edu.eci.arep.microspringboot.annotations.RestController;
import edu.eci.arep.microspringboot.classes.Task;

import java.util.List;

import static edu.eci.arep.microspringboot.classes.TaskManager.getTaskManager;

@RestController
@RequestMapping("/task")
public class TaskController {
    @GetMapping
    public static List<Task> getTasks(@RequestParam(value = "name", defaultValue = "All") String name) {
        if(name.equals("All")) return getTaskManager().getTasks();
        return getTaskManager().getTasksByName(name);
    }
}
