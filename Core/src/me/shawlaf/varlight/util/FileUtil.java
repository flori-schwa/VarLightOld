package me.shawlaf.varlight.util;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

public class FileUtil {

    private FileUtil() {
        throw new IllegalStateException("Instantiating Util class!");
    }

    public static String getExtension(File file) {
        String path = file.getAbsolutePath();

        return path.substring(path.lastIndexOf('.'));
    }

    public static boolean isDeflated(File file) throws IOException {
        boolean deflated = false;

        try (FileInputStream fis = new FileInputStream(file)) {
            DataInputStream dataInputStream = new DataInputStream(fis);

            int lsb = dataInputStream.readUnsignedByte();
            int msb = dataInputStream.readUnsignedByte();

            int read = (msb << 8) | lsb;

            if (read == GZIPInputStream.GZIP_MAGIC) {

                read = dataInputStream.readByte();

                if (read == 0x08) {
                    deflated = true;
                }
            }

            dataInputStream.close();
        }

        return deflated;
    }

}
