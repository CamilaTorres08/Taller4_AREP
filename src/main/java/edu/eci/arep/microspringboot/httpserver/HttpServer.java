package edu.eci.arep.microspringboot.httpserver;

import edu.eci.arep.microspringboot.annotations.GetMapping;
import edu.eci.arep.microspringboot.annotations.RequestMapping;
import edu.eci.arep.microspringboot.annotations.RestController;
import edu.eci.arep.microspringboot.classes.Task;

import java.net.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.eci.arep.microspringboot.classes.TaskManager.getTaskManager;
import static edu.eci.arep.microspringboot.helpers.ClassesConverter.findClasses;

public class HttpServer implements Runnable {
    public static Map<String,Map<String, Method>> services = new HashMap<>();
    final AtomicBoolean running = new AtomicBoolean(false);
    final ThreadPoolExecutor executor;
    private ServerSocket serverSocket;
    static String dir;
    private final int port;
    private String classPath = "edu.eci.arep";
    Thread t;

    public HttpServer(int portServer,int nThreads, int capacity, String filesPath, String classPath) {
        this.port = portServer;
        staticfiles(filesPath);
        setClassPath(classPath);
        preloadServices();
        this.executor = new ThreadPoolExecutor(
            nThreads, nThreads,
            0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(capacity),
            new ThreadFactory() {
                private final ThreadFactory delegate = Executors.defaultThreadFactory();
                private int idx = 0;
                @Override public Thread newThread(Runnable r) {
                    Thread t = delegate.newThread(r);
                    t.setName("http-worker-" + (++idx));
                    t.setDaemon(false);
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
    public Thread startAsync() {
        if (running.get()) {
            System.out.println("Servidor ya está corriendo");
            return t;
        }
        t = new Thread(this,"http-aceptor");
        t.start();
        return t;
    }

    @Override
    public void run() {
        if (!running.compareAndSet(false, true)) return;
        try(ServerSocket ss = new ServerSocket(this.port)){
            serverSocket = ss;
            ss.setReuseAddress(true);
            ss.setSoTimeout(1000);
            System.out.println("Server listening on port " + port);
            while (running.get()) {
                try{
                    Socket clientSocket = ss.accept();
                    clientSocket.setSoTimeout(1500);
                    executor.execute(() -> {
                        try {
                            runServer(clientSocket);
                        } catch (IOException e) {
                            System.err.println("Error running server");
                        }
                    });
                }catch (SocketTimeoutException e){}
                catch(SocketException e){
                    if(!running.get()) break;
                    System.err.println("SocketException: " + e.getMessage());
                }
            }
        }catch (IOException ex){
            System.err.println("Accept failed.");
        }finally {
            shutdown();
            running.set(false);
            System.out.println("Server stopped");
        }
    }
    public void stop(){
        if(!running.compareAndSet(true, false)) return;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        } catch (IOException ignored) {}
        shutdown();
    }
    private void shutdown(){
        executor.shutdown();
        try{
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                System.out.println("forcing task closure...");
                executor.shutdownNow();
                executor.awaitTermination(5, TimeUnit.SECONDS);
            }
        }catch(InterruptedException ie){
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    public void preloadServices(){
        try {
            Set<Class<?>> classes = findClasses(classPath);
            for (Class<?> c : classes) {
                loadServices(c);
            }
        }catch (ClassNotFoundException e) {
            System.err.println("Could not load class.");
        }catch (IOException e){
            System.err.println("Could not load: "+e.getMessage());
        }
    }
    /**
     * Starts the server
     * @throws IOException  if an I/O error occurs during server initialization
     */
    public static void runServer(Socket clientSocket) throws IOException {
        BufferedReader in = new BufferedReader(
                new InputStreamReader(
                        clientSocket.getInputStream()));
        OutputStream outputStream = clientSocket.getOutputStream();
        String inputLine, firstLine="";
        boolean isFirstLine = true;
        int contentLength = 0;
        while ((inputLine = in.readLine()) != null) {
            if (isFirstLine) {
                isFirstLine = false;
                firstLine = inputLine;
            }
            if (inputLine.toLowerCase().startsWith("content-length:")) {
                contentLength = Integer.parseInt(inputLine.split(":")[1].trim());
            }
            if (inputLine.isEmpty() || inputLine.trim().isEmpty()) {
                break;
            }
        }
        String body = "";
        if (contentLength > 0) {
            char[] bodyChars = new char[contentLength];
            in.read(bodyChars, 0, contentLength);
            body = new String(bodyChars);
        }
        if(!firstLine.isEmpty()) manageRequest(firstLine,body,outputStream);
        outputStream.close();
        in.close();
        clientSocket.close();
    }
    /**
     *Loads GET handlers methods from a given class if is annotated with @RestController and @RequestMapping annotations.
     * @param c the class to inspect.
     **/
    public static void loadServices(Class<?> c) {
        if(c.isAnnotationPresent(RestController.class)){
            Method[] methods = c.getDeclaredMethods();
            RequestMapping annotation = (RequestMapping) c.getAnnotation(RequestMapping.class);
            Map<String,Method> methodValues = new HashMap<>();
            for(Method m : methods){
                if(m.isAnnotationPresent(GetMapping.class)){
                    String mapping = m.getAnnotation(GetMapping.class).value();
                    methodValues.put(mapping, m);
                }
            }
            if(!methodValues.isEmpty()) services.put(annotation != null ? annotation.value() : "/app", methodValues);
        }
    }

    /**
     * Assigns the class path to search the classes
     */
    public void setClassPath(String path) {
        this.classPath = path;
    }
    /**
     * Processes an incoming GET request by resolving the target service and executing it.
     * @param requri the URI of the requested resource (must start with)
     * @return Response
     */
    private static HttpResponse invokeService(URI requri) throws InvocationTargetException, IllegalAccessException {
        HttpRequest req = new HttpRequest(requri);
        HttpResponse res = new HttpResponse();
        String basePath = req.getBasePath(services.keySet());
        if(basePath == null) return res.status(404).body("Base path not found");
        String resourcePath = req.getSourcePath(basePath);
        Method m = services.get(basePath).get(resourcePath);
        if(m == null) {
            return res.status(405).body("Service not found: "+ basePath+resourcePath);
        }
        String[] values = req.getParamValues(m);

        Object o = m.invoke(null,values);
        if(o == null){
            return res.status(204);
        }
        return res.status(200).body(o);
    }
    /**
     * Manages an HTTP request by processing the method, resource, and body,
     * and writing the corresponding response.
     * @param inputLine the first line of the HTTP request (contains method and resource)
     * @param body      the body of the request, if present
     * @param out       the output stream used to send the response back to the client
     * @throws IOException if an error occurs while writing to the output stream
     */
    private static void manageRequest(String inputLine, String body,OutputStream out) throws IOException {
        HttpResponse response = new HttpResponse();
        try {
            String[] dividedUri = inputLine.split(" ");
            URI requestUri = new URI(dividedUri[1]);
            String path = requestUri.getPath();
            String method = dividedUri[0];
            if(method.equals("POST")){
                String taskName = "";
                String taskDescription = "";
                String[] values = body.split(",");
                for(String value : values){
                    String[] pair = value.split(":",2);
                    String key = pair[0].replace("\"","").replace("{","").replace("}","").replace(" ","").trim();
                    String val = pair[1].replace("\"","").replace("{","").replace("}","").trim();
                    if(key.equals("name")) taskName = val;
                    if(key.equals("description")) taskDescription = val;
                }
                if(!taskName.isEmpty() && !taskDescription.isEmpty()) response = saveTask(taskName, taskDescription);
                else response = new HttpResponse(400,"Missing values, Task Name and Task Description are required");

            }else if(method.equals("GET") && (path.equals("/") || path.endsWith("html") || path.endsWith("js") || path.endsWith("css")
                    || path.endsWith("png") || path.endsWith("jpg") || path.endsWith("jpeg"))) {
                response = getResources(path);
            }
            else if (method.equals("GET")) {
                    response = invokeService(requestUri);
            }else{
                response = new HttpResponse(405,"Method "+method+" "+path+" not supported");
            }
        }catch (FileNotFoundException e){
            response = new HttpResponse(404,e.getMessage());
        }catch (Exception e) {
            response = new HttpResponse(500,e.getMessage());
        }finally {
            //if the response does not have content-type assign automatically text/plain
            response.getHeaders().putIfAbsent("Content-Type","text/plain");
            byte[] bodyResponse = response.getBody();
            //if response does not have body set status No Content
            if(bodyResponse == null && response.getStatusCode() == 200) response.setStatusCode(204);
            //Build full response
            StringBuilder sb = new StringBuilder()
                    .append("HTTP/1.1 ").append(response.getStatusCode()).append(" ").append(response.getStatusMessage()).append("\r\n");
            response.getHeaders().forEach((k,v) -> sb.append(k).append(": ").append(v).append("\r\n"));
            sb.append("\r\n");
            out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            //if response have body include it
            if(bodyResponse != null) out.write(response.getBody());
            out.flush();
        }
    }


    /**
     *Configures the directory for serving static files.
     * If the directory does not exist, it will be created automatically.
     * @param path the relative path where static files will be served from
     * @throws IOException if an error occurs while creating the directory
     * @throws IllegalArgumentException if the path is null, blank, not a directory, or not readable
     **/
    public static void staticfiles(String path) {
        try {
            if (path == null || path.isEmpty())
                throw new IllegalArgumentException("Static Files: path cannot be null/blank");
            String root = "src/main";
            Path configured = Paths.get(root + path);

            if (Files.exists(configured)) {
                if (!Files.isDirectory(configured)) {
                    throw new IllegalArgumentException("staticfiles: no es un directorio: " + configured);
                }
                if (!Files.isReadable(configured)) {
                    throw new IllegalArgumentException("staticfiles: directorio no legible: " + configured);
                }
            } else {
                Files.createDirectories(configured);
            }
            dir = root + path;
        } catch (Exception e) {
            System.err.println("Could not create static files: " + e);
        }
    }


    /**
     * Manage disk files.
     *
     * @param path resource of the request
     * @throws IOException if an error occurs while writing to the output stream
     * @return Response
     */
    private static HttpResponse getResources(String path) throws IOException {
        String fullPath = dir;
        if(path.equals("/")){
            fullPath += "/" + "pages/index.html";
        }
        else if(path.endsWith("html")){
            fullPath += "/" + "pages" + path;
        }else {
            fullPath += path;
        }
        if(fullPath.endsWith("html") || fullPath.endsWith("css") || fullPath.endsWith("js")){
            return sendTextFile(fullPath);
        }
        return sendImageFile(fullPath);
    }
    /**
     * Read text files (html, css and javascript).
     *
     * @param filePath full path of the file
     * @throws IOException if an error occurs while reading the file
     * @return File content
     */
    private static String readFile(String filePath) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
    /**
     * Gets the header based on the file extension.
     * @param path full path of the file
     * @return content-type header
     */
    private static String getHeader(String path){
        if (path.endsWith(".html") || path.endsWith(".htm")) return "text/html";
        if (path.endsWith(".css"))  return "text/css";
        if (path.endsWith(".js"))   return "application/javascript";
        if (path.endsWith(".png"))  return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
    }
    /**
     * Read and send the file text (html, css or javascript)
     * @param fullPath full path of the file
     * @throws IOException if an error occurs while writing to the output stream
     * @return Response with the text file
     */
    private static HttpResponse sendTextFile(String fullPath)throws IOException{
        byte[] output = readFile(fullPath).getBytes(StandardCharsets.UTF_8);
        return new HttpResponse(200,output).contentType(getHeader(fullPath));
    }
    /**
     * Read and send images (png, jpg or jpeg)
     * @param fullPath full path of the file
     * @throws IOException if an error occurs while reading the file image
     * @return Response with the image
     */
    private static HttpResponse sendImageFile(String fullPath)throws IOException {
        Path filePath = Paths.get(fullPath);
        if (!Files.exists(filePath)) {
            return new HttpResponse(404,"Image not found");
        }
        byte[] fileContent = Files.readAllBytes(filePath);
        return new HttpResponse(200,fileContent)
                .contentType(getHeader(fullPath))
                .header("Content-Length",String.valueOf(fileContent.length));
    }
    /**
     * Saves task in memory
     * Since POST endpoints are not yet implemented using lambdas
     * this method handles the task creation directly.
     *  It uses the singleton to add the task
     * @param name name of the task
     * @param description description of the task
     * @return Response with the created task
     */
    private static HttpResponse saveTask(String name, String description){
        Task newTask = getTaskManager().addTask(name,description);
        return new HttpResponse(200,newTask).contentType("application/json");
    }

}
