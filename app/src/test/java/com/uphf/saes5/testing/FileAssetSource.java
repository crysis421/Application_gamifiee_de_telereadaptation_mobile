package com.uphf.saes5.testing;

import com.uphf.saes5.data.storage.AssetSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Les vrais assets de l'application, lus sur le disque : les tests manipulent les modèles et
 * démonstrations réellement livrés.
 */
public final class FileAssetSource implements AssetSource {

    /** Dossier des assets, relatif au module {@code app} où tournent les tests. */
    public static final File APP_ASSETS = new File("src/main/assets");

    private final File root;

    public FileAssetSource(File root) {
        this.root = root;
    }

    public static FileAssetSource appAssets() {
        return new FileAssetSource(APP_ASSETS);
    }

    @Override
    public InputStream open(String path) throws IOException {
        return new FileInputStream(new File(root, path));
    }
}
