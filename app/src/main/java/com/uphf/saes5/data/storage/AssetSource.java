package com.uphf.saes5.data.storage;

import java.io.IOException;
import java.io.InputStream;

/**
 * Fichiers livrés avec l'application (modèles des exercices de base, démonstrations).
 *
 * <p>Interface plutôt qu'{@code AssetManager} directement : les dépôts qui lisent ces
 * fichiers se testent avec une source en mémoire.</p>
 */
public interface AssetSource {

    InputStream open(String path) throws IOException;
}
