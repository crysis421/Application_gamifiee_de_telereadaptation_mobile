package com.uphf.saes5.di;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.util.function.Supplier;

/**
 * Crée un ViewModel avec ses dépendances, passées par son constructeur.
 *
 * <p>Le {@code ViewModelProvider} n'appelle la fabrique qu'à la première demande : après une
 * rotation de l'écran, l'Activity recréée retrouve la même instance, et donc le même état.</p>
 *
 * <pre>{@code
 * viewModel = new ViewModelProvider(this, ViewModelFactory.of(HomeViewModel.class,
 *         () -> new HomeViewModel(container.gameSettings, ...))).get(HomeViewModel.class);
 * }</pre>
 */
public final class ViewModelFactory<V extends ViewModel> implements ViewModelProvider.Factory {

    private final Class<V> type;
    private final Supplier<V> creator;

    private ViewModelFactory(Class<V> type, Supplier<V> creator) {
        this.type = type;
        this.creator = creator;
    }

    public static <V extends ViewModel> ViewModelFactory<V> of(Class<V> type, Supplier<V> creator) {
        return new ViewModelFactory<>(type, creator);
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (!modelClass.isAssignableFrom(type)) {
            throw new IllegalArgumentException("Fabrique de " + type.getSimpleName()
                    + ", pas de " + modelClass.getSimpleName());
        }
        return modelClass.cast(creator.get());
    }
}
