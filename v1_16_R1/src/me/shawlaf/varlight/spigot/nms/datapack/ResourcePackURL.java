package me.shawlaf.varlight.spigot.nms.datapack;

import net.minecraft.server.v1_16_R1.EnumResourcePackType;
import net.minecraft.server.v1_16_R1.MinecraftKey;
import net.minecraft.server.v1_16_R1.ResourceNotFoundException;
import net.minecraft.server.v1_16_R1.ResourcePackAbstract;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ResourcePackURL extends ResourcePackAbstract {

    private final URL url;

    public ResourcePackURL(File var0, URL url) {
        super(var0);

        this.url = url;
    }

    private ZipInputStream inputStream() throws IOException {
        return new ZipInputStream(url.openStream());
    }

    @Override
    protected InputStream a(String s) throws IOException {
        ZipInputStream zipInputStream = inputStream();
        ZipEntry zipEntry;

        while ((zipEntry = zipInputStream.getNextEntry()) != null) {
            if (!zipEntry.getName().equals(s)) {
                zipInputStream.closeEntry();
                continue;
            }

            return zipInputStream;
        }

        throw new ResourceNotFoundException(this.a, s);
    }

    @Override
    protected boolean c(String s) {
        try (ZipInputStream zipInputStream = inputStream();) {
            ZipEntry zipEntry;

            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (!zipEntry.getName().equals(s)) {
                    zipInputStream.closeEntry();
                    continue;
                }

                return true;
            }

            return false;
        } catch (IOException e) {
            return false;
        }
    }


    @Override
    public Collection<MinecraftKey> a(EnumResourcePackType enumResourcePackType, String s, String s1, int i, Predicate<String> predicate) {
        return null;
    }

    @Override
    public Set<String> a(EnumResourcePackType enumResourcePackType) {
        return null;
    }

    @Override
    public void close() {

    }
}
