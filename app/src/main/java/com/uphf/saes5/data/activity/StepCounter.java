package com.uphf.saes5.data.activity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.fitness.FitnessLocal;
import com.google.android.gms.fitness.LocalRecordingClient;
import com.google.android.gms.fitness.data.LocalBucket;
import com.google.android.gms.fitness.data.LocalDataPoint;
import com.google.android.gms.fitness.data.LocalDataSet;
import com.google.android.gms.fitness.data.LocalDataType;
import com.google.android.gms.fitness.data.LocalField;
import com.google.android.gms.fitness.request.LocalDataReadRequest;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Nombre de pas du jour (US-5.1), via la Recording API de Google Play services.
 *
 * <p>Les pas sont comptés par l'appareil lui-même : pas de compte Google, rien n'est envoyé.
 * La Recording API ne compte qu'à partir de l'abonnement, d'où l'abonnement dès que la
 * permission est accordée.</p>
 *
 * <p>Trois cas d'absence sont distingués pour que l'accueil affiche un message utile plutôt
 * qu'un zéro trompeur : permission refusée, Play services absent ou trop ancien, lecture en
 * échec.</p>
 */
public final class StepCounter {

    private static final String TAG = "StepCounter";

    public enum Availability { AVAILABLE, PERMISSION_MISSING, UNSUPPORTED }

    public interface Callback {
        void onSteps(int steps);
        void onUnavailable(@NonNull Availability reason);
    }

    private final Context context;

    public StepCounter(Context context) {
        this.context = context.getApplicationContext();
    }

    public Availability availability() {
        int playServices = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                context, LocalRecordingClient.LOCAL_RECORDING_CLIENT_MIN_VERSION_CODE);
        if (playServices != ConnectionResult.SUCCESS) return Availability.UNSUPPORTED;
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
            return Availability.PERMISSION_MISSING;
        }
        return Availability.AVAILABLE;
    }

    /** Lit les pas depuis minuit. S'abonne au passage : sans abonnement, rien n'est compté. */
    public void readToday(@NonNull Callback callback) {
        Availability availability = availability();
        if (availability != Availability.AVAILABLE) {
            callback.onUnavailable(availability);
            return;
        }

        LocalRecordingClient client = FitnessLocal.getLocalRecordingClient(context);
        client.subscribe(LocalDataType.TYPE_STEP_COUNT_DELTA)
                .addOnFailureListener(error -> Log.w(TAG, "Abonnement aux pas impossible", error));

        long now = System.currentTimeMillis();
        LocalDataReadRequest request = new LocalDataReadRequest.Builder()
                .aggregate(LocalDataType.TYPE_STEP_COUNT_DELTA)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startOfDay(now), now, TimeUnit.MILLISECONDS)
                .build();

        client.readData(request)
                .addOnSuccessListener(response -> {
                    int steps = 0;
                    for (LocalBucket bucket : response.getBuckets()) {
                        for (LocalDataSet dataSet : bucket.getDataSets()) {
                            steps += sum(dataSet);
                        }
                    }
                    callback.onSteps(steps);
                })
                .addOnFailureListener(error -> {
                    Log.w(TAG, "Lecture des pas impossible", error);
                    callback.onUnavailable(Availability.UNSUPPORTED);
                });
    }

    private static int sum(LocalDataSet dataSet) {
        int steps = 0;
        for (LocalDataPoint point : dataSet.getDataPoints()) {
            for (LocalField field : point.getDataType().getFields()) {
                steps += point.getValue(field).asInt();
            }
        }
        return steps;
    }

    /** Minuit du jour de {@code nowMillis}, dans le fuseau de l'appareil. */
    static long startOfDay(long nowMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(nowMillis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}
