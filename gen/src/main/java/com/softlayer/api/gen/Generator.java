package com.softlayer.api.gen;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Generator {

    public static final String BASE_PKG = "com.softlayer.api.service";
    
    private final File dir;
    private final URL metadataUrl;
    private final Restriction whitelist;
    private final Restriction blacklist;

    /**
     * @param dir The directory to generate classes into.
     * @param metadataUrl The metadata to generate from.
     * @param whitelist
     * @param blacklist
     */
    public Generator(File dir, URL metadataUrl, Restriction whitelist, Restriction blacklist) {
        this.dir = dir;
        this.metadataUrl = metadataUrl;
        this.whitelist = whitelist;
        this.blacklist = blacklist;
    }
    
    public void buildClient() throws IOException {
        log("Deleting existing service files");
        recursivelyDelete(new File(dir, "com/softlayer/api/service"));
        
        log("Loading metadata");
        Meta meta = Meta.fromUrl(metadataUrl);
        applyRestrictions(meta);
        
        log("Generating source code");
        List<TypeClass> classes = new ArrayList<>(meta.types.size());
        for (Meta.Type type : meta.types.values()) {
            TypeClass typeClass = new MetaConverter(BASE_PKG, meta, type).buildTypeClass();
            ClassWriter.emitType(dir, typeClass, meta);
            classes.add(typeClass);
        }
        ClassWriter.emitPackageInfo(dir, classes);
        log("Done");
    }
    
    public void applyRestrictions(Meta meta) {
        if (whitelist != null) {
            if (!whitelist.types.isEmpty()) {
                meta.types.keySet().retainAll(whitelist.types);
            }
            for (Meta.Type type : meta.types.values()) {
                Set<String> properties = whitelist.properties.get(type.name);
                if (properties != null) {
                    type.properties.keySet().retainAll(properties);
                }
                Set<String> methods = whitelist.methods.get(type.name);
                if (methods != null) {
                    type.methods.keySet().retainAll(methods);
                }
            }
        }
        if (blacklist != null) {
            meta.types.keySet().removeAll(blacklist.types);
            for (Meta.Type type : meta.types.values()) {
                Set<String> properties = blacklist.properties.get(type.name);
                if (properties != null) {
                    type.properties.keySet().removeAll(properties);
                }
                Set<String> methods = blacklist.methods.get(type.name);
                if (methods != null) {
                    type.methods.keySet().removeAll(methods);
                }
            }
        }
    }
    
    protected void log(String contents) {
        System.out.println(contents);
    }

    public void recursivelyDelete(File file) throws IOException {
        if (!file.exists()) {
            return;
        }
        Files.walkFileTree(file.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
