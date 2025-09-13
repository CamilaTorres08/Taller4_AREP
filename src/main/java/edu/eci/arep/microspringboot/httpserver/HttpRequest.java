/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.eci.arep.microspringboot.httpserver;

import edu.eci.arep.microspringboot.annotations.RequestParam;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 *
 * @author andrea.torres-g
 */
public class HttpRequest {

    URI uri;
    Map<String, String> parameters = new HashMap<>();
    public HttpRequest(URI requri){
        this.uri = requri;
        if(uri.getQuery() != null) setParamValues();
    }

    /**
     * Stores the parameters
     */
    public void setParamValues(){
        String[] values = uri.getQuery().split("&");
        for(String value : values){
            String[] keyValue = value.split("=");
            if(keyValue.length > 1){
                parameters.put(keyValue[0], keyValue[1]);
            }
        }
    }
    /**
     * Determines the base path of the current request URI from a set of known paths.
     * @param paths the set of available base paths
     * @return the matching base path if found, otherwise null
     */
    public String getBasePath(Set<String> paths){
        String fullPath = uri.getPath();
        Optional<String> path = paths.stream().filter(x -> fullPath.startsWith(x)).findFirst();
        if(path.isPresent()){
            return path.get();
        }
        return null;
    }
    /**
     * Extracts the resource path from the request URI by removing the given base path.
     * @param basePath the base path to strip from the URI
     * @return the remaining resource path, or "/" if empty
     */
    public String getSourcePath(String basePath){
        String resourcePath = uri.getPath().substring(basePath.length());
        return resourcePath.isEmpty() ? "/" : resourcePath;
    }

    /**
     * Retrieves the value of a query parameter
     * @param paraName the name of the parameter to look up
     * @return the parameter value
     */
    public String getValues(String paraName){
        return parameters.get(paraName);
    }
    /**
     * Retrieves the parameter values for a given method by inspecting its @RequestParam annotations.
     * @param m the method whose parameters will be analyzed
     * @return an array of parameter values extracted from the request or their default values
     */
    public String[] getParamValues(Method m){
        Annotation[][] annotations = m.getParameterAnnotations();
        String[] argsValues = new String[annotations.length];
        for (int i = 0; i < annotations.length; i++) {
            RequestParam requestParam = null;
            for (Annotation annotation : annotations[i]) {
                if (annotation instanceof RequestParam r) {
                    requestParam = r;
                    break;
                }
            }
            if (requestParam != null) {
                String value = getValues(requestParam.value());
                if(value != null) argsValues[i] = value;
                else argsValues[i] = requestParam.defaultValue();
            }
        }
        if(argsValues.length > 0) return argsValues;
        return null;
    }
}
