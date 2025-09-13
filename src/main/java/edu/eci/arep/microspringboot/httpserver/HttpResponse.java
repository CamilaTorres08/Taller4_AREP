/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.eci.arep.microspringboot.httpserver;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static edu.eci.arep.microspringboot.helpers.JsonConverter.toJson;

/**
 *
 * @author andrea.torres-g
 */
public class HttpResponse {
    int statusCode=200;
    String statusMessage="OK";
    Map<String, String> headers = new LinkedHashMap<>();
    byte[] body;
    public HttpResponse(int statusCode, Object body) {
        this.statusCode = statusCode;
        this.body = toByte(body);
        setStatusMessage();
    }
    public HttpResponse(){}
    /**
     *Set the status code of the response
     * @param statusCode HTTP code of the response
     * @return HttpResponse object
     */
    public HttpResponse status(int statusCode) {
        this.statusCode = statusCode;
        setStatusMessage();
        return this;
    }
    /**
     *Save headers of the response
     * @param name header name
     * @param value header value
     * @return HttpResponse object
     */
    public HttpResponse header(String name, String value) {
        headers.put(name, value);
        return this;
    }
    /**
     *Set the body of the response
     * @param body object to response
     * @return HttpResponse object
     */
    public HttpResponse body(Object body) {
        this.body = toByte(body);
        return this;
    }

    public int getStatusCode() {
        return statusCode;
    }
    public Map<String, String> getHeaders() {
        return headers;
    }
    public String getStatusMessage() {
        return statusMessage;
    }
    public byte[] getBody() {
        return body;
    }
    /**
     *Set the value of content-type header
     * @param v content-type value
     * @return HttpResponse object
     **/
    public HttpResponse contentType(String v){
        return header("Content-Type", v);
    }
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
        setStatusMessage();
    }
    /**
     *Gets the Status Message based on the Status Code
     **/
    public void setStatusMessage() {
        switch (statusCode) {
            case 200:
                statusMessage = "OK";
                break;
            case 204:
                statusMessage = "No Content";
                break;
            case 400:
                statusMessage = "Bad Request";
                break;
            case 401:
                statusMessage = "Unauthorized";
                break;
            case 403:
                statusMessage = "Forbidden";
                break;
            case 404:
                statusMessage = "Not Found";
                break;
            case 405:
                statusMessage = "Method Not Allowed";
                break;
            case 406:
                statusMessage = "Not Acceptable";
                break;
            default:
                statusMessage = "Internal Server Error";
                break;
        }
    }

    /**
     * Converts the body object to bytes
     * @param obj The object to convert
     * @return object converted to byte
     */
    private byte[] toByte(Object obj){
        if(obj instanceof byte[]){
            headers.putIfAbsent("Content-Type","application/octet-stream");
            return (byte[])obj;
        }
        else if(obj instanceof String){
            return ((String)obj).getBytes(StandardCharsets.UTF_8);
        }
        //if the object is type json, and does not have content.type header, set application/json automatically
        headers.putIfAbsent("Content-Type","application/json");
        return toJson(obj).getBytes(StandardCharsets.UTF_8);
    }
    
}
