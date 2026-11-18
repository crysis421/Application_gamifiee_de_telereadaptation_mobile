import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Génère les trois sons de retour de l'application (US-3.3).
 *
 * Synthèse simple : des sinusoïdes additionnées, avec une enveloppe attaque/déclin pour
 * éviter les clics en début et fin de fichier. 44,1 kHz, 16 bits, mono — format que
 * SoundPool lit nativement.
 *
 * Usage : java GenerateSounds.java <dossier_de_sortie>
 */
public class GenerateSounds {

    private static final int SAMPLE_RATE = 44100;

    public static void main(String[] args) throws IOException {
        Path out = Paths.get(args.length > 0 ? args[0] : ".");
        Files.createDirectories(out);

        // Validation : deux notes montantes, brèves et claires (La5 -> Mi6).
        double[] valid = concat(
                tone(880.00, 0.090, 0.55, 0.004, 0.030),
                tone(1318.51, 0.150, 0.55, 0.004, 0.090));

        // Erreur : deux notes descendantes dans le grave, sans agressivité (Mi4 -> La3).
        double[] failed = concat(
                tone(329.63, 0.110, 0.50, 0.006, 0.040),
                tone(220.00, 0.200, 0.50, 0.006, 0.140));

        // Fin d'exercice : arpège majeur ascendant (Do5, Mi5, Sol5, Do6), accord final tenu.
        double[] finish = concat(
                tone(523.25, 0.110, 0.45, 0.005, 0.045),
                tone(659.25, 0.110, 0.45, 0.005, 0.045),
                tone(783.99, 0.110, 0.45, 0.005, 0.045),
                chord(new double[] {1046.50, 1318.51}, 0.420, 0.45, 0.005, 0.330));

        write(out.resolve("feedback_valid.wav"), valid);
        write(out.resolve("feedback_failed.wav"), failed);
        write(out.resolve("exercise_finish.wav"), finish);
    }

    /** Une note : fondamentale plus une octave discrète, pour un timbre moins nu. */
    private static double[] tone(double hz, double seconds, double gain,
                                 double attack, double release) {
        return chord(new double[] {hz}, seconds, gain, attack, release);
    }

    private static double[] chord(double[] frequencies, double seconds, double gain,
                                  double attack, double release) {
        int count = (int) (seconds * SAMPLE_RATE);
        double[] samples = new double[count];
        for (int i = 0; i < count; i++) {
            double t = (double) i / SAMPLE_RATE;
            double value = 0;
            for (double hz : frequencies) {
                value += Math.sin(2 * Math.PI * hz * t);
                // Harmonique à l'octave, en retrait : adoucit la sinusoïde pure.
                value += 0.18 * Math.sin(4 * Math.PI * hz * t);
            }
            value /= frequencies.length * 1.18;
            samples[i] = value * gain * envelope(t, seconds, attack, release);
        }
        return samples;
    }

    /** Enveloppe trapézoïdale : sans elle, les bords produisent un clic audible. */
    private static double envelope(double t, double total, double attack, double release) {
        if (t < attack) {
            return t / attack;
        }
        double fadeStart = total - release;
        if (t > fadeStart) {
            return Math.max(0, (total - t) / release);
        }
        return 1.0;
    }

    private static double[] concat(double[]... parts) {
        int total = 0;
        for (double[] part : parts) {
            total += part.length;
        }
        double[] result = new double[total];
        int at = 0;
        for (double[] part : parts) {
            System.arraycopy(part, 0, result, at, part.length);
            at += part.length;
        }
        return result;
    }

    private static void write(Path path, double[] samples) throws IOException {
        int dataSize = samples.length * 2;
        ByteBuffer pcm = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN);
        for (double sample : samples) {
            double clamped = Math.max(-1.0, Math.min(1.0, sample));
            pcm.putShort((short) Math.round(clamped * Short.MAX_VALUE));
        }

        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(path.toFile()))) {
            out.writeBytes("RIFF");
            out.write(le32(36 + dataSize));
            out.writeBytes("WAVE");
            out.writeBytes("fmt ");
            out.write(le32(16));                 // taille du bloc fmt
            out.write(le16(1));                  // PCM non compressé
            out.write(le16(1));                  // mono
            out.write(le32(SAMPLE_RATE));
            out.write(le32(SAMPLE_RATE * 2));    // octets par seconde
            out.write(le16(2));                  // alignement de bloc
            out.write(le16(16));                 // bits par échantillon
            out.writeBytes("data");
            out.write(le32(dataSize));
            out.write(pcm.array());
        }
        System.out.printf("  %-24s %6.0f ms  %6d octets%n",
                path.getFileName(), samples.length * 1000.0 / SAMPLE_RATE, 44 + dataSize);
    }

    private static byte[] le32(int value) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
    }

    private static byte[] le16(int value) {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort((short) value).array();
    }
}
