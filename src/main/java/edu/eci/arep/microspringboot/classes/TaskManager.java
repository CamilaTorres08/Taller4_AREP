package edu.eci.arep.microspringboot.classes;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TaskManager {
    List<Task> tasks;
    int lastId;
    static TaskManager taskManager;
    public TaskManager() {
        this.tasks = new ArrayList<Task>();
        this.lastId = 0;
    }
    public static TaskManager getTaskManager(){
        if(taskManager == null){
            taskManager = new TaskManager();
        }
        return taskManager;
    }
    public Task addTask(String name, String description) {
        lastId++;
        Task task = new Task(name, description, lastId);
        this.tasks.add(task);
        return task;

    }
    public List<Task> getTasks() {
        return tasks;
    }
    public List<Task> getTasksByName(String name) {
        return this.tasks.stream().filter(x -> x.getName().toLowerCase().contains(name.toLowerCase())).collect(Collectors.toList());
    }
}
