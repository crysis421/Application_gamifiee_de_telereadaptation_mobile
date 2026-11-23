package com.uphf.saes5;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Notice et consentement de recherche (US-9.1). */
public class ConsentTest {

    @Test
    public void neverAccepted_requiresConsent() {
        assertTrue(Consent.isRequired(Consent.NONE, Consent.CURRENT_VERSION));
    }

    @Test
    public void acceptedCurrentNotice_doesNotAskAgain() {
        assertFalse(Consent.isRequired(Consent.CURRENT_VERSION, Consent.CURRENT_VERSION));
    }

    @Test
    public void aNewerNotice_requiresConsentAgain() {
        // Changer ce que l'application collecte impose un nouvel accord éclairé.
        assertTrue(Consent.isRequired(1, 2));
    }

    @Test
    public void withoutStorage_consentIsNotRequired() {
        // Hors application (tests unitaires) : ne jamais bloquer sur un écran.
        assertFalse(Consent.isRequired());
    }
}
