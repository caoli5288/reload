package com.mengcraft.reload.util;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import org.bukkit.Bukkit;
import sun.misc.Unsafe;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.logging.Logger;

public class Artifact {

    // constants
    private static final String MAVEN_HOME = System.getProperty("maven.home", System.getProperty("user.home") + "/.m2");
    private static final String MAVEN_LOCAL_REPO = MAVEN_HOME + "/repository";
    private static final String MAVEN_REPOSITORY = System.getProperty("maven.repository", "https://maven.aliyun.com/repository/public");
    private static final String ARTIFACT_PATH = "/%GROUP_PATH%/%ARTIFACT%/%VERSION%/%ARTIFACT%-%VERSION%.jar";
    // utils
    private static final MethodHandle MH_addURL = _addURL();
    private static final Logger LOGGER = Bukkit.getLogger();
    // stats
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final File file;

    Artifact(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        file = new File(MAVEN_LOCAL_REPO + ARTIFACT_PATH
                .replace("%GROUP_PATH%", groupId.replace('.', '/'))
                .replace("%ARTIFACT%", artifactId)
                .replace("%VERSION%", version));
    }

    private static MethodHandle _addURL() {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Unsafe unsafe = (Unsafe) f.get(Unsafe.class);
            f = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            long offset = unsafe.staticFieldOffset(f);
            MethodHandles.Lookup lookup = (MethodHandles.Lookup) unsafe.getObject(MethodHandles.Lookup.class, offset);
            lookup = lookup.in(URLClassLoader.class);
            return lookup.findVirtual(URLClassLoader.class, "addURL", MethodType.methodType(void.class, URL.class));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Artifact of(String groupId, String artifactId, String version) {
        return new Artifact(groupId, artifactId, version);
    }

    public static Artifact of(String descriptor) {
        String[] desc = descriptor.split(":");
        return new Artifact(desc[0], desc[1], desc[2]);
    }

    public static void load(ClassLoader cl, List<String> artifacts) throws IOException {
        URLClassLoader ucl = (URLClassLoader) cl;
        for (String artifact : artifacts) {
            of(artifact).load(ucl);
        }
    }

    public void load(URLClassLoader cl) throws IOException {
        LOGGER.info("Load " + groupId + ":" + artifactId + ":" + version);
        init();
        try {
            MH_addURL.invokeExact(cl, file.toURI().toURL());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public File getFile() {
        return file;
    }

    void init() throws IOException {
        // return if file exists
        if (file.exists()) {
            return;
        }
        LOGGER.info("Init " + file);
        // ensure parents
        Files.createParentDirs(file);
        // temp file
        File tmp = File.createTempFile("tmp", ".tmp", file.getParentFile());
        tmp.deleteOnExit();
        // download from maven repository
        URL url = new URL(MAVEN_REPOSITORY + ARTIFACT_PATH
                .replace("%GROUP_PATH%", groupId.replace('.', '/'))
                .replace("%ARTIFACT%", artifactId)
                .replace("%VERSION%", version));
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        try {
            httpConn.addRequestProperty("User-Agent", "Mozilla/5.0 (compatible; Java; x64)");
            httpConn.connect();
            try (InputStream from = httpConn.getInputStream()) {
                try (FileOutputStream to = new FileOutputStream(tmp)) {
                    ByteStreams.copy(from, to);
                }
            }
            Files.move(tmp, file);
        } finally {
            httpConn.disconnect();
        }
    }
}
