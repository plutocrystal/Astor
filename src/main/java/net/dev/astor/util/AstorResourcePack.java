package net.dev.astor.util;

import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.util.ResourceLocation;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;

public final class AstorResourcePack implements IResourcePack {
    public static final String DOMAIN = "astor";
    private static final String ROOT = "assets/" + DOMAIN + "/";
    private static final Set<String> DOMAINS = Collections.singleton(DOMAIN);
    private static final AstorResourcePack INSTANCE = new AstorResourcePack();

    private AstorResourcePack() {
    }

    public static AstorResourcePack getInstance() {
        return INSTANCE;
    }

    private static String toResourcePath(ResourceLocation location) {
        return ROOT + location.getResourcePath();
    }

    @Override
    public InputStream getInputStream(ResourceLocation location) throws IOException {
        if (!DOMAIN.equals(location.getResourceDomain())) {
            throw new FileNotFoundException(location.toString());
        }
        InputStream stream = ResourceUtil.open(toResourcePath(location));
        if (stream == null) {
            throw new FileNotFoundException(location.toString());
        }
        return stream;
    }

    @Override
    public boolean resourceExists(ResourceLocation location) {
        if (!DOMAIN.equals(location.getResourceDomain())) {
            return false;
        }

        return ResourceUtil.class.getClassLoader().getResource(toResourcePath(location)) != null
                && ResourceUtil.open(toResourcePath(location)) != null;
    }

    @Override
    public Set<String> getResourceDomains() {
        return DOMAINS;
    }

    @Override
    public <T extends IMetadataSection> T getPackMetadata(IMetadataSerializer metadataSerializer, String metadataSectionName) throws IOException {
        throw new FileNotFoundException("astor has no metadata section " + metadataSectionName);
    }

    @Override
    public BufferedImage getPackImage() throws IOException {
        throw new FileNotFoundException("astor has no pack image");
    }

    @Override
    public String getPackName() {
        return "astor";
    }

    @Override
    public String toString() {
        return "astor resource pack";
    }
}