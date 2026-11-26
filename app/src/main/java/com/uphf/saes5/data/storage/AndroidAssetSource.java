package com.uphf.saes5.data.storage;

import android.content.res.AssetManager;

import java.io.IOException;
import java.io.InputStream;

/** {@link AssetSource} adossé au dossier {@code assets/} de l'application. */
public final class AndroidAssetSource implements AssetSource {

    private final AssetManager assets;

    public AndroidAssetSource(AssetManager assets) {
        this.assets = assets;
    }

    @Override
    public InputStream open(String path) throws IOException {
        return assets.open(path);
    }
}
