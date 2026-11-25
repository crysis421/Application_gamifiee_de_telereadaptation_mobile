import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Génère les modèles de référence des exercices de base, livrés dans
 * {@code app/src/main/assets/movements}.
 *
 * <p>Un modèle est d'ordinaire enregistré en filmant quelqu'un. Ceux-ci sont calculés : un
 * squelette en 3D est animé articulation par articulation, puis projeté comme le verrait la
 * caméra du téléphone, de face ou de profil selon l'exercice. Le résultat a le format d'un
 * enregistrement (33 points MediaPipe, centrés sur les hanches, en longueurs de torse, une
 * image toutes les 100 ms) : l'application ne fait pas la différence.</p>
 *
 * <p>La comparaison ne retient que huit angles — coudes, épaules, hanches, genoux — vus dans
 * l'image : seuls comptent donc les mouvements de ces articulations et l'angle de vue.
 * Chaque modèle décrit une répétition : pose de départ tenue, mouvement, retour tenu.</p>
 *
 * <p>Usage : {@code java tools/GenerateReferenceMovements.java app/src/main/assets/movements}</p>
 */
public class GenerateReferenceMovements {

    static final int SAMPLE_MS = 100;
    static final double HOLD_SEC = 0.8;

    // Longueurs des segments, en longueurs de torse (milieu des épaules → milieu des hanches).
    static final double SHOULDER_HALF = 0.40, HIP_HALF = 0.22;
    static final double UPPER_ARM = 0.62, FOREARM = 0.52, THIGH = 0.95, SHIN = 0.90;
    /** Distance de la caméra, en longueurs de torse (≈ 3 m) : légère perspective. */
    static final double CAMERA_DISTANCE = 6.0;

    enum View { FRONT, SIDE }

    enum ElbowPlane {
        /** Le coude plie vers l'avant du corps (flexion, pompes, rowing). */
        SAGITTAL,
        /** Le coude plie dans le plan du corps, avant-bras vers le haut (développé). */
        FRONTAL
    }

    /** Une pose : angles en degrés, gauche (L) et droite (R). */
    static final class Pose {
        double trunkPitch;      // inclinaison du corps vers l'avant (90 = à plat, face au sol)
        double spineRoll;       // inclinaison latérale du buste seul (+ = vers la gauche)
        double shFlexL, shFlexR = 0;   // bras vers l'avant (+) ou l'arrière (-)
        double shAbdL = 8, shAbdR = 8; // bras écarté sur le côté
        double elbowL = 5, elbowR = 5; // flexion du coude
        double hipFlexL, hipFlexR;     // cuisse vers l'avant (+) ou l'arrière (-)
        double hipAbdL = 4, hipAbdR = 4;
        double kneeL = 3, kneeR = 3;   // flexion du genou

        Pose copy() {
            Pose p = new Pose();
            p.trunkPitch = trunkPitch; p.spineRoll = spineRoll;
            p.shFlexL = shFlexL; p.shFlexR = shFlexR; p.shAbdL = shAbdL; p.shAbdR = shAbdR;
            p.elbowL = elbowL; p.elbowR = elbowR;
            p.hipFlexL = hipFlexL; p.hipFlexR = hipFlexR; p.hipAbdL = hipAbdL; p.hipAbdR = hipAbdR;
            p.kneeL = kneeL; p.kneeR = kneeR;
            return p;
        }

        Pose with(Consumer<Pose> change) {
            Pose p = copy();
            change.accept(p);
            return p;
        }

        // Raccourcis symétriques.
        Pose arms(double flex, double abd, double elbow) {
            return with(p -> { p.shFlexL = p.shFlexR = flex; p.shAbdL = p.shAbdR = abd;
                p.elbowL = p.elbowR = elbow; });
        }

        Pose legs(double hipFlex, double hipAbd, double knee) {
            return with(p -> { p.hipFlexL = p.hipFlexR = hipFlex; p.hipAbdL = p.hipAbdR = hipAbd;
                p.kneeL = p.kneeR = knee; });
        }

        Pose pitch(double degrees) {
            return with(p -> p.trunkPitch = degrees);
        }

        static Pose lerp(Pose a, Pose b, double t) {
            double s = (1 - Math.cos(Math.PI * t)) / 2; // départ et arrivée en douceur
            Pose p = new Pose();
            p.trunkPitch = mix(a.trunkPitch, b.trunkPitch, s);
            p.spineRoll = mix(a.spineRoll, b.spineRoll, s);
            p.shFlexL = mix(a.shFlexL, b.shFlexL, s); p.shFlexR = mix(a.shFlexR, b.shFlexR, s);
            p.shAbdL = mix(a.shAbdL, b.shAbdL, s); p.shAbdR = mix(a.shAbdR, b.shAbdR, s);
            p.elbowL = mix(a.elbowL, b.elbowL, s); p.elbowR = mix(a.elbowR, b.elbowR, s);
            p.hipFlexL = mix(a.hipFlexL, b.hipFlexL, s); p.hipFlexR = mix(a.hipFlexR, b.hipFlexR, s);
            p.hipAbdL = mix(a.hipAbdL, b.hipAbdL, s); p.hipAbdR = mix(a.hipAbdR, b.hipAbdR, s);
            p.kneeL = mix(a.kneeL, b.kneeL, s); p.kneeR = mix(a.kneeR, b.kneeR, s);
            return p;
        }

        static double mix(double a, double b, double s) {
            return a + (b - a) * s;
        }
    }

    /** Un temps clé : la pose à atteindre, en {@code seconds} depuis le temps clé précédent. */
    record Key(double seconds, Pose pose) {}

    record Movement(String id, View view, ElbowPlane elbows, Pose start, List<Key> keys) {}

    static Movement movement(String id, View view, ElbowPlane elbows, Pose start, Key... keys) {
        return new Movement(id, view, elbows, start, List.of(keys));
    }

    static Key key(double seconds, Pose pose) {
        return new Key(seconds, pose);
    }

    static final Pose STANDING = new Pose();

    static List<Movement> movements() {
        List<Movement> list = new ArrayList<>();

        // ---- Jambes ----
        // Assis-debout, de profil : assis bras croisés, on se penche, on se lève, on se rassoit.
        Pose seated = STANDING.pitch(12).arms(25, 10, 125).legs(100, 6, 95);
        list.add(movement("base_assis_debout", View.SIDE, ElbowPlane.SAGITTAL, seated,
                key(0.6, seated.pitch(40).legs(115, 6, 95)),
                key(0.9, STANDING.arms(25, 10, 125)),
                key(0.9, seated.pitch(38).legs(112, 6, 92)),
                key(0.6, seated)));

        // Squat, de face : genoux écartés, buste penché, bras tendus devant pour l'équilibre.
        Pose squatBottom = STANDING.pitch(28).arms(85, 12, 5).legs(110, 22, 105);
        list.add(movement("base_squat", View.FRONT, ElbowPlane.SAGITTAL, STANDING,
                key(1.2, squatBottom), key(1.2, STANDING)));

        // Fentes alternées, de profil : une répétition = une fente de chaque côté.
        Pose lungeLeftFront = STANDING.with(p -> {
            p.hipFlexL = 85; p.kneeL = 90;      // jambe avant
            p.hipFlexR = -25; p.kneeR = 95;     // jambe arrière, genou vers le sol
        });
        Pose lungeRightFront = STANDING.with(p -> {
            p.hipFlexR = 85; p.kneeR = 90;
            p.hipFlexL = -25; p.kneeL = 95;
        });
        list.add(movement("base_fentes_alternees", View.SIDE, ElbowPlane.SAGITTAL, STANDING,
                key(1.0, lungeLeftFront), key(1.0, STANDING),
                key(1.0, lungeRightFront), key(1.0, STANDING)));

        // ---- Bras ----
        // Flexion des coudes, de profil : avant-bras montés vers les épaules.
        list.add(movement("base_flexion_coudes", View.SIDE, ElbowPlane.SAGITTAL,
                STANDING.arms(0, 8, 8),
                key(1.0, STANDING.arms(5, 8, 140)), key(1.0, STANDING.arms(0, 8, 8))));

        // Développé au-dessus de la tête, de face : des coudes en « poteau » aux bras tendus.
        Pose goalpost = STANDING.arms(0, 90, 90);
        list.add(movement("base_developpe_tete", View.FRONT, ElbowPlane.FRONTAL, goalpost,
                key(1.0, STANDING.arms(0, 165, 8)), key(1.0, goalpost)));

        // Dips sur chaise, de profil : mains sur l'assise derrière soi, on descend en pliant les coudes.
        Pose dipsTop = STANDING.arms(-30, 6, 5).legs(90, 6, 80);
        list.add(movement("base_dips_chaise", View.SIDE, ElbowPlane.SAGITTAL, dipsTop,
                key(1.1, dipsTop.arms(-75, 6, 85).legs(100, 6, 85)), key(1.1, dipsTop)));

        // ---- Pectoraux ----
        // Pompes contre le mur, de profil : corps droit incliné, mains au mur.
        Pose wallTop = STANDING.pitch(18).arms(78, 10, 5);
        list.add(movement("base_pompes_mur", View.SIDE, ElbowPlane.SAGITTAL, wallTop,
                key(1.0, STANDING.pitch(26).arms(40, 12, 95)), key(1.0, wallTop)));

        // Pompes sur les genoux, de profil.
        Pose kneeTop = STANDING.pitch(55).arms(55, 10, 5).legs(0, 4, 35);
        list.add(movement("base_pompes_genoux", View.SIDE, ElbowPlane.SAGITTAL, kneeTop,
                key(1.1, STANDING.pitch(70).arms(15, 12, 95).legs(0, 4, 20)), key(1.1, kneeTop)));

        // Pompes, de profil : corps gainé des épaules aux talons.
        Pose pushTop = STANDING.pitch(74).arms(74, 10, 5);
        list.add(movement("base_pompes", View.SIDE, ElbowPlane.SAGITTAL, pushTop,
                key(1.1, STANDING.pitch(84).arms(22, 12, 95)), key(1.1, pushTop)));

        // ---- Dos ----
        // Élévations en Y, de face : bras tendus, montés en Y au-dessus de la tête.
        list.add(movement("base_elevations_y", View.FRONT, ElbowPlane.SAGITTAL,
                STANDING.arms(5, 12, 5),
                key(1.2, STANDING.arms(25, 150, 5)), key(1.2, STANDING.arms(5, 12, 5))));

        // Rowing élastique, de profil : bras tendus devant, coudes tirés vers l'arrière.
        Pose rowStart = STANDING.pitch(15).arms(65, 8, 5).legs(15, 6, 12);
        list.add(movement("base_rowing_elastique", View.SIDE, ElbowPlane.SAGITTAL, rowStart,
                key(1.0, rowStart.arms(-15, 10, 100)), key(1.0, rowStart)));

        // Charnière de hanche (good morning), de profil : mains sur la poitrine, buste basculé.
        Pose hingeTop = STANDING.arms(30, 10, 130).legs(0, 6, 8);
        list.add(movement("base_charniere_hanche", View.SIDE, ElbowPlane.SAGITTAL, hingeTop,
                key(1.3, hingeTop.pitch(75).legs(75, 6, 22)), key(1.3, hingeTop)));

        // ---- Tronc ----
        // Montées de genoux, de profil : un genou vers la poitrine, puis l'autre.
        // (Une flexion latérale du buste avait été essayée : vue de face, elle ne change qu'une
        // hanche d'une trentaine de degrés, trop peu pour que le moteur la distingue de la
        // pose de départ.)
        Pose kneeArms = STANDING.arms(15, 10, 85);
        list.add(movement("base_montees_genoux", View.SIDE, ElbowPlane.SAGITTAL, kneeArms,
                key(0.9, kneeArms.with(p -> { p.hipFlexL = 110; p.kneeL = 110; })), key(0.9, kneeArms),
                key(0.9, kneeArms.with(p -> { p.hipFlexR = 110; p.kneeR = 110; })), key(0.9, kneeArms)));

        // Pont fessier, de profil : allongé sur le dos, bassin soulevé. Seules les hanches
        // bougent franchement : bras et genoux varient de moins de 20°, sans quoi ils
        // dilueraient le mouvement et le pont ne se distinguerait plus de la pose au sol.
        Pose bridgeDown = STANDING.pitch(-90).arms(10, 10, 5).legs(65, 6, 112);
        list.add(movement("base_pont_fessier", View.SIDE, ElbowPlane.SAGITTAL, bridgeDown,
                key(1.1, STANDING.pitch(-65).arms(10, 10, 5).legs(0, 6, 98)), key(1.1, bridgeDown)));

        // Planche, de profil : posture tenue, sur les avant-bras. Légère respiration.
        Pose plank = STANDING.pitch(80).arms(80, 10, 90);
        list.add(movement("base_planche", View.SIDE, ElbowPlane.SAGITTAL, plank,
                key(1.0, plank.with(p -> p.trunkPitch = 81.5)),
                key(1.0, plank.with(p -> p.trunkPitch = 79)),
                key(1.0, plank)));
        return list;
    }

    // ------------------------------------------------------------------ cinématique

    record V(double x, double y, double z) {
        V plus(V o) { return new V(x + o.x, y + o.y, z + o.z); }
        V times(double k) { return new V(x * k, y * k, z * k); }
        double dot(V o) { return x * o.x + y * o.y + z * o.z; }
        V cross(V o) { return new V(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x); }
        V neg() { return times(-1); }
    }

    /** Rotation de {@code v} autour de l'axe unitaire {@code a} (formule de Rodrigues). */
    static V rot(V v, V a, double degrees) {
        double t = Math.toRadians(degrees), c = Math.cos(t), s = Math.sin(t);
        return v.times(c).plus(a.cross(v).times(s)).plus(a.times(a.dot(v) * (1 - c)));
    }

    /** Les 33 points MediaPipe d'une pose, en 3D (x à gauche du sujet, y en haut, z devant lui). */
    static V[] skeleton(Pose p, ElbowPlane elbows) {
        V lateral = new V(1, 0, 0);
        // Bassin : seulement incliné vers l'avant.
        V up = rot(new V(0, 1, 0), lateral, p.trunkPitch);
        V forward = rot(new V(0, 0, 1), lateral, p.trunkPitch);
        // Buste : le bassin, plus l'inclinaison latérale de la colonne.
        V spineUp = rot(up, forward, -p.spineRoll);
        V spineSide = rot(lateral, forward, -p.spineRoll);

        V[] pts = new V[33];
        V hipMid = new V(0, 0, 0);
        V shoulderMid = spineUp;
        for (int side = 0; side < 2; side++) {
            double s = side == 0 ? 1 : -1; // 0 = gauche (indices impairs MediaPipe), 1 = droite
            boolean left = side == 0;
            double shFlex = left ? p.shFlexL : p.shFlexR, shAbd = left ? p.shAbdL : p.shAbdR;
            double elbow = left ? p.elbowL : p.elbowR;
            double hipFlex = left ? p.hipFlexL : p.hipFlexR, hipAbd = left ? p.hipAbdL : p.hipAbdR;
            double knee = left ? p.kneeL : p.kneeR;

            V shoulder = shoulderMid.plus(spineSide.times(SHOULDER_HALF * s));
            V arm = sagittal(frontal(spineUp.neg(), forward, s * shAbd), spineSide, shFlex);
            V elbowPt = shoulder.plus(arm.times(UPPER_ARM));
            V fore = elbows == ElbowPlane.SAGITTAL
                    ? sagittal(arm, spineSide, elbow)
                    : frontal(arm, forward, s * elbow);
            V wrist = elbowPt.plus(fore.times(FOREARM));

            V hip = hipMid.plus(lateral.times(HIP_HALF * s));
            V thigh = sagittal(frontal(up.neg(), forward, s * hipAbd), lateral, hipFlex);
            V kneePt = hip.plus(thigh.times(THIGH));
            V shin = sagittal(thigh, lateral, -knee);
            V ankle = kneePt.plus(shin.times(SHIN));
            V foot = sagittal(shin, lateral, 90);

            int o = left ? 0 : 1;
            pts[11 + o] = shoulder;
            pts[13 + o] = elbowPt;
            pts[15 + o] = wrist;
            pts[17 + o] = wrist.plus(fore.times(0.10)).plus(spineSide.times(0.03 * s)); // auriculaire
            pts[19 + o] = wrist.plus(fore.times(0.12));                                 // index
            pts[21 + o] = wrist.plus(fore.times(0.06)).plus(spineSide.times(-0.03 * s)); // pouce
            pts[23 + o] = hip;
            pts[25 + o] = kneePt;
            pts[27 + o] = ankle;
            pts[29 + o] = ankle.plus(foot.times(-0.07));  // talon
            pts[31 + o] = ankle.plus(foot.times(0.22));   // pointe du pied
        }
        // Tête.
        V headUp = spineUp, headFwd = rot(forward, lateral, 0);
        V nose = shoulderMid.plus(headUp.times(0.42)).plus(headFwd.times(0.12));
        pts[0] = nose;
        for (int side = 0; side < 2; side++) {
            double s = side == 0 ? 1 : -1;
            int o = side;
            V eye = nose.plus(headUp.times(0.04)).plus(spineSide.times(0.035 * s));
            pts[1 + 3 * o] = eye.plus(spineSide.times(-0.015 * s)); // coin intérieur
            pts[2 + 3 * o] = eye;                                   // œil
            pts[3 + 3 * o] = eye.plus(spineSide.times(0.015 * s));  // coin extérieur
            pts[7 + o] = shoulderMid.plus(headUp.times(0.38)).plus(spineSide.times(0.12 * s));
            pts[9 + o] = nose.plus(headUp.times(-0.05)).plus(spineSide.times(0.03 * s));
        }
        return pts;
    }

    /** Flexion vers l'avant (+) : rotation dans le plan sagittal, -haut → avant → haut. */
    static V sagittal(V v, V lateral, double degrees) {
        return rot(v, lateral, -degrees);
    }

    /** Écartement : rotation dans le plan du corps, vers l'extérieur pour s·angle > 0. */
    static V frontal(V v, V forward, double degrees) {
        return rot(v, forward, degrees);
    }

    /** Projection perspective, puis normalisation identique à {@code MovementMath.normalize}. */
    static float[] project(V[] pts, View view) {
        double[] x = new double[33], y = new double[33];
        for (int i = 0; i < 33; i++) {
            V p = pts[i];
            // De face, la caméra est devant le sujet (z) ; de profil, à sa gauche (x).
            double depth = view == View.FRONT ? p.z() : p.x();
            double k = CAMERA_DISTANCE / (CAMERA_DISTANCE - depth);
            x[i] = (view == View.FRONT ? -p.x() : p.z()) * k;
            y[i] = -p.y() * k; // y de l'image vers le bas
        }
        double hipX = (x[23] + x[24]) / 2, hipY = (y[23] + y[24]) / 2;
        double shX = (x[11] + x[12]) / 2, shY = (y[11] + y[12]) / 2;
        double torso = Math.hypot(shX - hipX, shY - hipY);
        float[] out = new float[66];
        for (int i = 0; i < 33; i++) {
            out[2 * i] = round4((x[i] - hipX) / torso);
            out[2 * i + 1] = round4((y[i] - hipY) / torso);
        }
        return out;
    }

    static float round4(double v) {
        return Math.round(v * 10000.0) / 10000f;
    }

    static List<float[]> frames(Movement m) {
        List<float[]> frames = new ArrayList<>();
        int hold = (int) Math.round(HOLD_SEC * 1000 / SAMPLE_MS);
        for (int i = 0; i < hold; i++) frames.add(project(skeleton(m.start(), m.elbows()), m.view()));
        Pose from = m.start();
        for (Key k : m.keys()) {
            int steps = (int) Math.round(k.seconds() * 1000 / SAMPLE_MS);
            for (int i = 1; i <= steps; i++) {
                Pose p = Pose.lerp(from, k.pose(), i / (double) steps);
                frames.add(project(skeleton(p, m.elbows()), m.view()));
            }
            from = k.pose();
        }
        for (int i = 0; i < hold; i++) frames.add(project(skeleton(from, m.elbows()), m.view()));
        return frames;
    }

    static String toJson(String name, List<float[]> frames) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"name\":\"").append(name).append("\",\"sampleIntervalMs\":").append(SAMPLE_MS)
                .append(",\"durationSec\":").append((int) Math.ceil(frames.size() * SAMPLE_MS / 1000.0))
                .append(",\"landmarkCount\":33,\"frames\":[");
        for (int f = 0; f < frames.size(); f++) {
            if (f > 0) sb.append(',');
            sb.append('[');
            float[] fr = frames.get(f);
            for (int i = 0; i < fr.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(String.format(Locale.ROOT, "%.4f", fr[i]));
            }
            sb.append(']');
        }
        return sb.append("]}").toString();
    }

    public static void main(String[] args) throws IOException {
        Path dir = Path.of(args.length > 0 ? args[0] : "app/src/main/assets/movements");
        Files.createDirectories(dir);
        for (Movement m : movements()) {
            List<float[]> frames = frames(m);
            Files.writeString(dir.resolve(m.id() + ".json"), toJson(m.id(), frames),
                    StandardCharsets.UTF_8);
            System.out.printf("%-24s %-5s %3d images%n", m.id(), m.view(), frames.size());
        }
    }
}
