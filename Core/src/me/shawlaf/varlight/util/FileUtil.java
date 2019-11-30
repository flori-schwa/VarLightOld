package me.shawlaf.varlight.util;

import java.io.File;

public class FileUtil {

    private FileUtil() {
        throw new IllegalStateException("Instantiating Util class!");
    }

    public static String getExtension(File file) {
        String path = file.getAbsolutePath();

        return path.substring(path.lastIndexOf('.'));
    }

}
