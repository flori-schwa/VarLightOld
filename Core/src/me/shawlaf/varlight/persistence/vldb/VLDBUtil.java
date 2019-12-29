package me.shawlaf.varlight.persistence.vldb;

import me.shawlaf.varlight.persistence.ICustomLightSource;

import java.nio.charset.StandardCharsets;

public class VLDBUtil {

    public static final int SIZEOF_BYTE = 1;
    public static final int SIZEOF_INT16 = 2;
    public static final int SIZEOF_UINT24 = 3;
    public static final int SIZEOF_INT32 = 4;

    public static final int SIZEOF_MAGIC = SIZEOF_INT32;
    public static final int SIZEOF_HEADER_WITHOUT_OFFSET_TABLE = SIZEOF_MAGIC + (2 * SIZEOF_INT32) + SIZEOF_INT16;
    public static final int SIZEOF_OFFSET_TABLE_ENTRY = SIZEOF_INT16 + SIZEOF_INT32;

    public static final int SIZEOF_CHUNK_WITHOUT_LIGHT_DATA = SIZEOF_INT16 + SIZEOF_UINT24;
    public static final int SIZEOF_LIGHT_SOURCE_WITHOUT_ASCII = SIZEOF_INT16 + SIZEOF_BYTE;

    private VLDBUtil() {
        throw new IllegalStateException("Cannot create instance of util class!");
    }

    public static int sizeofHeader(int amountChunks) {
        return SIZEOF_HEADER_WITHOUT_OFFSET_TABLE + (amountChunks * SIZEOF_OFFSET_TABLE_ENTRY);
    }

    public static int sizeofChunk(ICustomLightSource[] chunk) {
        int size = SIZEOF_CHUNK_WITHOUT_LIGHT_DATA;

        for (ICustomLightSource lightSource : chunk) {
            size += sizeofLightSource(lightSource);
        }

        return size;
    }

    public static int sizeofLightSource(ICustomLightSource lightSource) {
        return SIZEOF_LIGHT_SOURCE_WITHOUT_ASCII + sizeofASCII(lightSource.getType());
    }

    public static int sizeofASCII(String ascii) {
        return SIZEOF_INT16 + ascii.getBytes(StandardCharsets.US_ASCII).length;
    }

}
