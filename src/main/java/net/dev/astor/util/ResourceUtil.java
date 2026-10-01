package net.dev.astor.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public final class ResourceUtil {
    private ResourceUtil() {
    }

    public static List<String> list(String directory) {
        String path = normalize(directory);
        if (path.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> names = new LinkedHashSet<>();
        try {
            Enumeration<URL> directories = ResourceUtil.class.getClassLoader().getResources(path);
            while (directories.hasMoreElements()) {
                URL url = directories.nextElement();
                try {
                    if ("file".equals(url.getProtocol())) {
                        addDirectory(toPath(url), names);
                    } else if ("jar".equals(url.getProtocol())) {
                        addJar(url, path, names);
                    } else {
                        System.out.println(String.format("Skipping unsupported classpath entry %s", url));
                    }
                } catch (Exception e) {
                    System.out.println(String.format("Skipping classpath entry %s: %s", url, e));
                }
            }
        } catch (IOException e) {
            System.out.println(String.format("Could not look up %s: %s", path, e));
        }
        return new ArrayList<>(names);
    }

    public static InputStream open(String path) {
        String normalized = normalize(path);
        if (normalized.isEmpty()) {
            return null;
        }
        InputStream stream = ResourceUtil.class.getClassLoader().getResourceAsStream(normalized);
        return stream == null ? ResourceUtil.class.getResourceAsStream("/" + normalized) : stream;
    }

    private static String normalize(String directory) {
        String normalized = directory.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static void addDirectory(Path directory, Set<String> names) throws IOException {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (Stream<Path> files = Files.list(directory)) {
            files.filter(Files::isRegularFile)
                    .map(directory::relativize)
                    .map(Path::toString)
                    .forEach(names::add);
        }
    }

    private static void addJar(URL url, String path, Set<String> names) throws IOException {
        URLConnection connection = url.openConnection();
        if (!(connection instanceof JarURLConnection)) {
            throw new IOException("not a jar connection");
        }
        JarURLConnection jarConnection = (JarURLConnection) connection;
        jarConnection.setUseCaches(false);
        String prefix = path + "/";
        try (JarFile jar = jarConnection.getJarFile()) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                String entry = entries.nextElement().getName();
                if (!entry.startsWith(prefix)) {
                    continue;
                }
                String name = entry.substring(prefix.length());
                if (!name.isEmpty() && name.indexOf('/') < 0) {
                    names.add(name);
                }
            }
        }
    }

    private static Path toPath(URL url) throws IOException {
        try {
            return Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            return Paths.get(url.getPath());
        }
    }
}