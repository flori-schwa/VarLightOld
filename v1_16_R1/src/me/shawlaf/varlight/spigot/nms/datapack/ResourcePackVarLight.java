package me.shawlaf.varlight.spigot.nms.datapack;

import com.google.common.collect.Lists;
import me.shawlaf.varlight.spigot.VarLightPlugin;
import net.minecraft.server.v1_16_R1.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.CodeSource;
import java.util.*;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ResourcePackVarLight extends ResourcePackAbstract {

    private final VarLightPlugin plugin;

    public ResourcePackVarLight(VarLightPlugin plugin) {
        super(new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().getPath()));

        this.plugin = plugin;
    }

    private URL getResource(String path) {
        return plugin.getClass().getResource("/datapack/" + path);
    }

    private ZipInputStream getZipInputStream() throws IOException {
        CodeSource codeSource = plugin.getClass().getProtectionDomain().getCodeSource();

        if (codeSource == null) {
            throw new RuntimeException("Failed to get code source");
        }

        URL jar = codeSource.getLocation();
        return new ZipInputStream(jar.openStream());
    }

    @Override
    protected InputStream a(String s) throws IOException {
        URL url = getResource(s);

        if (url == null) {
            throw new ResourceNotFoundException(this.a, s);
        } else {
            return url.openStream();
        }
    }

    @Override
    protected boolean c(String s) {
        return getResource(s) != null;
    }

    @Override
    public Collection<MinecraftKey> a(EnumResourcePackType type, String namespace, String s1, int i, Predicate<String> predicate) {
        ZipInputStream zipInputStream;

        try {
            zipInputStream = getZipInputStream();
        } catch (IOException e) {
            return Collections.emptySet();
        }

        List<MinecraftKey> resources = Lists.newArrayList();
        String basePath = type.a() + "/" + namespace + "/";
        String path = basePath + s1 + "/";

        try {
            while (true) {
                ZipEntry next = zipInputStream.getNextEntry();

                if (next == null) {
                    break;
                }

                if (!next.getName().startsWith("datapack/")) {
                    zipInputStream.closeEntry();
                    continue;
                }

                if (!next.isDirectory()) {
                    String name = next.getName();
                    name = name.substring("datapack/".length());

                    if (!name.endsWith(".mcmeta") && name.startsWith(path)) {
                        String var12 = name.substring(basePath.length());
                        String[] var13 = var12.split("/");

                        if (var13.length >= i + 1 && predicate.test(var13[var13.length - 1])) {
                            resources.add(new MinecraftKey(namespace, var12));
                        }
                    }
                }

                zipInputStream.closeEntry();
            }

            return resources;
        } catch (IOException e) {
            new RuntimeException().printStackTrace(); // For debugging
            return Collections.emptySet();
        }
    }

    @Override
    public Set<String> a(EnumResourcePackType type) {
        ZipInputStream zipInputStream;

        try {
            zipInputStream = getZipInputStream();
        } catch (IOException e) {
            return Collections.emptySet();
        }

        Set<String> namespaces = new HashSet<>();

        try {
            while (true) {
                ZipEntry entry = zipInputStream.getNextEntry();

                if (entry == null) {
                    break;
                }

                String name = entry.getName();

                if (!name.startsWith("datapack/")) {
                    zipInputStream.closeEntry();
                    continue;
                }

                name = name.substring("datapack/".length());

                if (name.startsWith(type.a() + "/")) {
                    List<String> var6 = Lists.newArrayList(ResourcePackFile.b.split(name));

                    if (var6.size() > 1) {
                        String var7 = var6.get(1);

                        if (var7.equals(var7.toLowerCase(Locale.ROOT))) {
                            namespaces.add(var7);
                        } else {
                            this.d(var7);
                        }
                    }
                }
            }
        } catch (IOException e) {
            new RuntimeException().printStackTrace(); // For debugging
            return Collections.emptySet();
        }

        return namespaces;
    }

    @Override
    public void close() {

    }
}
