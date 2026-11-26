package com.uphf.saes5.domain.exercise;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import com.uphf.saes5.data.exercise.ExerciseRepository;
import com.uphf.saes5.domain.model.Exercise;
import com.uphf.saes5.domain.model.Movement;

/**
 * Exercices de base livrés avec l'application : un par difficulté pour chaque groupe
 * musculaire.
 *
 * <p>Chacun a son modèle de référence dans {@code assets/movements/<id>.json}, calculé par
 * {@code tools/GenerateReferenceMovements.java} — un exercice n'existe pas sans modèle. Le
 * modèle a été calculé pour un angle de vue précis, de face ou de profil : la consigne dit
 * donc comment placer le téléphone, sans quoi la comparaison ne peut pas reconnaître le
 * mouvement.</p>
 *
 * <p>Ils sont ajoutés une seule fois par {@link ExerciseRepository#init}, sans remplacer un
 * exercice existant du même nom ni le modèle qu'un utilisateur aurait enregistré.</p>
 *
 * <p>Chacun a aussi sa démonstration, dans {@code assets/demos/<id>.json} : le même mouvement
 * vu sous l'angle qui le rend le plus lisible, et posé au sol, pour le mannequin de l'écran
 * d'exercice. Le modèle, lui, est vu comme le filmera le téléphone — de face, un squat se lit
 * mal.</p>
 */
public final class BaseExercises {

    /** Dossier des modèles dans les assets ; le fichier porte l'identifiant de l'exercice. */
    public static final String ASSET_DIRECTORY = "movements";
    /** Dossier des démonstrations dans les assets, mêmes noms de fichiers. */
    public static final String DEMO_DIRECTORY = "demos";

    private static final String FACE = "Téléphone posé face à vous, le corps entier dans le cadre. ";
    private static final String SIDE = "Placez-vous de profil par rapport au téléphone, "
            + "le corps entier dans le cadre. ";

    private BaseExercises() {
    }

    /**
     * La démonstration à montrer pour un exercice : celle de l'exercice de base de même
     * identifiant, ou à défaut de même nom — un « Squat » enregistré par l'utilisateur avant
     * l'arrivée des exercices de base garde son propre modèle, mais profite de la
     * démonstration du Squat de base.
     *
     * @return le chemin de l'asset, ou {@code null} : l'exercice montre alors son propre modèle
     */
    @Nullable
    public static String demoAssetFor(Exercise exercise) {
        if (exercise == null) return null;
        for (Exercise base : all()) {
            if (base.getId().equals(exercise.getId())
                    || base.getName().trim().equalsIgnoreCase(exercise.getName().trim())) {
                return DEMO_DIRECTORY + "/" + base.getId() + ".json";
            }
        }
        return null;
    }

    /** Charge la démonstration d'un exercice, ou {@code null} s'il n'en a pas ou qu'elle est illisible. */
    @Nullable
    public static Movement loadDemo(Context context, Exercise exercise) {
        String asset = demoAssetFor(exercise);
        if (asset == null) return null;
        try (Reader reader = new InputStreamReader(
                context.getAssets().open(asset), StandardCharsets.UTF_8)) {
            return new Gson().fromJson(reader, Movement.class);
        } catch (IOException | RuntimeException e) {
            Log.w("BaseExercises", "Démonstration illisible : " + asset, e);
            return null;
        }
    }

    public static List<Exercise> all() {
        return Arrays.asList(
                // Jambes
                exercise("base_assis_debout", "Assis-debout", "Jambes", "Facile", 5, "Chaise", 10,
                        SIDE + "Assis au bord d'une chaise, bras croisés sur la poitrine. Penchez le "
                                + "buste en avant et levez-vous sans les mains, puis rasseyez-vous "
                                + "doucement."),
                exercise("base_squat", "Squat", "Jambes", "Moyen", 6, "Aucun", 12,
                        FACE + "Pieds écartés à la largeur des épaules. Descendez en poussant les "
                                + "fesses en arrière, bras tendus devant vous, genoux dans l'axe des "
                                + "pieds, puis remontez."),
                exercise("base_fentes_alternees", "Fentes alternées", "Jambes", "Difficile", 7,
                        "Aucun", 10,
                        SIDE + "Faites un grand pas en avant et fléchissez les deux genoux jusqu'à "
                                + "l'angle droit, puis revenez. Alternez les jambes : chaque fente "
                                + "compte une répétition."),
                // Bras
                exercise("base_flexion_coudes", "Flexion des coudes", "Bras", "Facile", 5,
                        "Bouteilles d'eau (facultatif)", 12,
                        SIDE + "Debout, bras le long du corps. Pliez les coudes pour monter les "
                                + "mains vers les épaules, sans bouger les coudes, puis redescendez."),
                exercise("base_developpe_tete", "Développé au-dessus de la tête", "Bras", "Moyen",
                        6, "Aucun", 12,
                        FACE + "Coudes à hauteur d'épaules, pliés à angle droit, mains vers le haut. "
                                + "Tendez les bras au-dessus de la tête, puis revenez."),
                exercise("base_dips_chaise", "Dips sur chaise", "Bras", "Difficile", 6, "Chaise", 10,
                        SIDE + "Mains sur le bord d'une chaise stable derrière vous, pieds devant. "
                                + "Descendez en pliant les coudes vers l'arrière, puis poussez pour "
                                + "remonter."),
                // Pectoraux
                exercise("base_pompes_mur", "Pompes contre le mur", "Pectoraux", "Facile", 5, "Mur",
                        12,
                        SIDE + "Mains au mur à hauteur d'épaules, corps droit et légèrement incliné. "
                                + "Approchez la poitrine du mur en pliant les coudes, puis repoussez."),
                exercise("base_pompes_genoux", "Pompes sur les genoux", "Pectoraux", "Moyen", 6,
                        "Tapis", 10,
                        SIDE + "Genoux au sol, mains sous les épaules, corps droit des genoux à la "
                                + "tête. Descendez la poitrine vers le sol, puis poussez."),
                exercise("base_pompes", "Pompes", "Pectoraux", "Difficile", 7, "Tapis", 10,
                        SIDE + "Mains sous les épaules, corps gainé des épaules aux talons. "
                                + "Descendez jusqu'à frôler le sol, puis poussez pour remonter."),
                // Dos
                exercise("base_elevations_y", "Élévations en Y", "Dos", "Facile", 5, "Aucun", 12,
                        FACE + "Bras tendus le long du corps. Montez-les en Y au-dessus de la tête "
                                + "en serrant les omoplates, puis redescendez lentement."),
                exercise("base_rowing_elastique", "Rowing élastique", "Dos", "Moyen", 6, "Élastique",
                        12,
                        SIDE + "Élastique tenu bras tendus devant vous, buste légèrement penché. "
                                + "Tirez les coudes vers l'arrière en serrant les omoplates, puis "
                                + "relâchez en contrôlant."),
                exercise("base_charniere_hanche", "Charnière de hanche", "Dos", "Difficile", 6,
                        "Aucun", 10,
                        SIDE + "Mains sur la poitrine, genoux légèrement fléchis. Basculez le buste "
                                + "vers l'avant en gardant le dos droit, puis redressez-vous."),
                // Tronc
                exercise("base_montees_genoux", "Montées de genoux", "Tronc", "Facile", 5, "Aucun",
                        12,
                        SIDE + "Debout, dos droit, coudes pliés. Montez un genou vers la poitrine, "
                                + "reposez le pied, puis l'autre genou. Une répétition = un genou de "
                                + "chaque côté."),
                exercise("base_pont_fessier", "Pont fessier", "Tronc", "Moyen", 6, "Tapis", 12,
                        SIDE + "Allongé sur le dos, genoux pliés, pieds au sol. Soulevez le bassin "
                                + "jusqu'à aligner épaules, hanches et genoux, puis redescendez."),
                exercise("base_planche", "Planche", "Tronc", "Difficile", 4, "Tapis", 6,
                        SIDE + "En appui sur les avant-bras et la pointe des pieds, corps aligné des "
                                + "épaules aux talons. Tenez la position : chaque répétition compte "
                                + "environ cinq secondes de maintien.")
        );
    }

    private static Exercise exercise(String id, String name, String muscle, String difficulty,
                                     int minutes, String equipment, int reps, String description) {
        return new Exercise(id, name, muscle, difficulty, minutes, equipment,
                starsFor(difficulty), description, reps, id,
                Exercise.DEFAULT_SIMILARITY_THRESHOLD);
    }

    private static int starsFor(String difficulty) {
        switch (difficulty) {
            case "Facile":
                return 2;
            case "Moyen":
                return 3;
            default:
                return 4;
        }
    }
}
