package com.uphf.saes5.ui.consent;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.uphf.saes5.data.session.SessionRepository;
import com.uphf.saes5.data.settings.ConsentRepository;
import com.uphf.saes5.ui.common.Event;

/**
 * Notice d'information et consentement de recherche (US-9.1).
 *
 * <ul>
 *   <li>Premier lancement (ou notice modifiée) : il faut accepter pour entrer dans
 *   l'application.</li>
 *   <li>Relecture depuis les réglages : le consentement peut être retiré, ce qui efface les
 *   séances enregistrées sur le téléphone.</li>
 * </ul>
 */
public final class ConsentViewModel extends ViewModel {

    public enum ConsentEvent {
        /** Consentement donné : entrer dans l'application. */
        OPEN_HOME,
        /** Consentement retiré, journal effacé : fermer l'application. */
        CLOSE_APP
    }

    private final ConsentRepository consent;
    private final SessionRepository sessions;
    private final boolean reviewMode;
    private final MutableLiveData<Event<ConsentEvent>> events = new MutableLiveData<>();

    /** @param review ouverture depuis les réglages, pour relire la notice */
    public ConsentViewModel(ConsentRepository consent, SessionRepository sessions, boolean review) {
        this.consent = consent;
        this.sessions = sessions;
        // Notice modifiée depuis l'accord : on la présente comme au premier lancement.
        this.reviewMode = review && !consent.isRequired();
    }

    public LiveData<Event<ConsentEvent>> getEvents() {
        return events;
    }

    public boolean isReviewMode() {
        return reviewMode;
    }

    /** Date d'acceptation, ou 0. */
    public long acceptedAt() {
        return consent.acceptedAt();
    }

    public void accept() {
        consent.accept();
        events.setValue(new Event<>(ConsentEvent.OPEN_HOME));
    }

    public void withdraw() {
        sessions.clear();
        consent.withdraw();
        events.setValue(new Event<>(ConsentEvent.CLOSE_APP));
    }
}
