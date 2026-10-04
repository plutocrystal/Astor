package net.dev.astor.module;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class ModuleScanner {
    public static final String IMPL_PACKAGE = "net.dev.astor.module.impl";
    private static final String IMPL_PATH = IMPL_PACKAGE.replace('.', '/') + "/";
    private static final String CLASS_SUFFIX = ".class";

    private ModuleScanner() {
    }

    public static List<Class<? extends Module>> findModules() {
        List<Class<? extends Module>> modules = new ArrayList<>();
        List<String> unreadable = new ArrayList<>();
        for (String className : findClassNames()) {
            Class<?> clazz = load(className);
            if (clazz == null) {
                unreadable.add(className);
            } else if (Module.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers())) {
                modules.add(clazz.asSubclass(Module.class));
            }
        }
        if (!unreadable.isEmpty()) {
            System.out.println(String.format("Skipped %d unreadable classes below %s, first was %s",
                    unreadable.size(), IMPL_PACKAGE, unreadable.get(0)));
        }
        modules.sort(Comparator.comparing(Class::getName));
        return modules;
    }

    public static Category categoryOf(Class<? extends Module> clazz) {
        String name = clazz.getName();
        if (!name.startsWith(IMPL_PACKAGE + ".")) {
            return null;
        }
        int end = name.indexOf('.', IMPL_PACKAGE.length() + 1);
        if (end < 0) {
            return null;
        }
        String simpleName = name.substring(IMPL_PACKAGE.length() + 1, end).toUpperCase(Locale.ROOT);
        for (Category category : Category.values()) {
            if (category.name().equals(simpleName)) {
                return category;
            }
        }
        return null;
    }

    private static Set<String> findClassNames() {
        Set<String> classNames = new LinkedHashSet<>();
        collectResources(classNames);
        collectCodeSource(classNames);
        return classNames;
    }

    private static void collectResources(Set<String> classNames) {
        Enumeration<URL> resources;
        try {
            resources = ModuleScanner.class.getClassLoader().getResources(IMPL_PATH);
        } catch (IOException e) {
            System.out.println("Could not look up " + IMPL_PATH + ": " + e);
            return;
        }
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            try {
                if ("file".equals(url.getProtocol())) {
                    Path packageDirectory = toPath(url);
                    walkDirectory(packageDirectory, packageDirectory, classNames);
                } else if ("jar".equals(url.getProtocol())) {
                    walkJar(url, classNames);
                } else {
                    System.out.println("Skipping unsupported classpath entry " + url);
                }
            } catch (Exception e) {
                System.out.println("Skipping classpath entry " + url + ": " + e);
            }
        }
    }

    private static void collectCodeSource(Set<String> classNames) {
        URL location;
        try {
            location = ModuleScanner.class.getProtectionDomain().getCodeSource().getLocation();
        } catch (Exception e) {
            System.out.println("Could not locate the classes of " + ModuleScanner.class.getName() + ": " + e);
            return;
        }
        if (location == null) {
            return;
        }
        try {
            URL file = unwrap(location);
            if (!"file".equals(file.getProtocol())) {
                System.out.println("Skipping unsupported code source " + location);
                return;
            }
            Path root = toPath(file);
            if (Files.isDirectory(root)) {
                walkDirectory(root.resolve(IMPL_PATH), root, classNames);
            } else {
                walkArchive(root, classNames);
            }
        } catch (Exception e) {
            System.out.println("Skipping code source " + location + ": " + e);
        }
    }

    private static URL unwrap(URL url) throws IOException {
        String spec = url.toString();
        if (spec.startsWith("jar:")) {
            spec = spec.substring("jar:".length());
        }
        int separator = spec.indexOf("!/");
        if (separator >= 0) {
            spec = spec.substring(0, separator);
        }
        return new URL(spec);
    }

    private static Path toPath(URL url) throws IOException {
        try {
            return Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            return Paths.get(url.getPath());
        }
    }

    private static void walkDirectory(Path packageDirectory, Path root, Set<String> classNames) throws IOException {
        if (!Files.isDirectory(packageDirectory)) {
            return;
        }
        try (Stream<Path> files = Files.walk(packageDirectory)) {
            files.filter(Files::isRegularFile)
                    .map(root::relativize)
                    .map(Path::toString)
                    .forEach(path -> addClassName(path, classNames));
        }
    }

    private static void walkJar(URL url, Set<String> classNames) throws IOException {
        URLConnection connection = url.openConnection();
        if (!(connection instanceof JarURLConnection)) {
            throw new IOException("not a jar connection");
        }
        JarURLConnection jarConnection = (JarURLConnection) connection;
        jarConnection.setUseCaches(false);
        try (JarFile jar = jarConnection.getJarFile()) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                addClassName(entries.nextElement().getName(), classNames);
            }
        }
    }

    private static void walkArchive(Path archive, Set<String> classNames) throws IOException {
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                addClassName(entries.nextElement().getName(), classNames);
            }
        }
    }

    private static void addClassName(String path, Set<String> classNames) {
        String normalized = path.replace('\\', '/');
        if (!normalized.startsWith(IMPL_PATH) || !normalized.endsWith(CLASS_SUFFIX) || normalized.indexOf('$') >= 0) {
            return;
        }
        classNames.add(normalized.substring(0, normalized.length() - CLASS_SUFFIX.length()).replace('/', '.'));
    }

    private static Class<?> load(String className) {
        try {
            return Class.forName(className, false, ModuleScanner.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
    }
}