package edu.eci.arep.microspringboot.helpers;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassesConverter {

    /**
     * Scans the classpath for all .class files under the given base package and
     * loads them into a Set<Class<?>>.
     * Supports two common URL protocols returned by the ClassLoader:
     *  - "file": classes present on the file system
     *  - "jar" : classes packaged inside a JAR
     */
    public static Set<Class<?>> findClasses(String basePackage) throws IOException, ClassNotFoundException {
        Set<Class<?>> classes = new HashSet<>();
        String packagePath = basePackage.replace('.', '/');
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = cl.getResources(packagePath);

        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            String protocol = url.getProtocol();

            if ("file".equals(protocol)) {
                String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                File dir = new File(filePath);
                if (dir.exists() && dir.isDirectory()) {
                    Files.walk(dir.toPath())
                            .filter(p -> p.toString().endsWith(".class"))
                            .forEach(p -> {
                                String className = toClassName(p, packagePath);
                                try {
                                    Class<?> cls = Class.forName(className, false, cl);
                                    classes.add(cls);
                                } catch (ClassNotFoundException ignored) { }
                            });
                }
            } else if ("jar".equals(protocol)) {
                JarURLConnection conn = (JarURLConnection) url.openConnection();
                try (JarFile jar = conn.getJarFile()) {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String name = entry.getName();
                        if (name.startsWith(packagePath) && name.endsWith(".class") && !entry.isDirectory()) {
                            String className = name.replace('/', '.').substring(0, name.length() - 6);
                            Class<?> cls = Class.forName(className, false, cl);
                            classes.add(cls);
                        }
                    }
                }
            }
        }
        return classes;
    }

    /**
     * Converts a filesystem path to a compiled .class file into a fully-qualified
     * Java class name that Class.forName can load.
     * @param classFilePath path to the .class file
     * @param packagePath the resource-form path of the base package
     * @return the fully-qualified class name
     */
    private static String toClassName(Path classFilePath, String packagePath) {
        String full = classFilePath.toString().replace(File.separatorChar, '/');
        int idx = full.indexOf(packagePath);
        String className = full.substring(idx).replace('/', '.');
        return className.substring(0, className.length() - ".class".length());
    }
}
