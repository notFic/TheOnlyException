package org.example.utils;

import java.net.URL;

public class ResourceLoader {

    public static URL getResource(String resourcePath) {
        URL resource = ResourceLoader.class.getResource(resourcePath);
        if (resource == null) {
            System.err.println("Resource not found: " + resourcePath);
            throw new IllegalStateException("Resource not found: " + resourcePath);
        }
        return resource;
    }
}