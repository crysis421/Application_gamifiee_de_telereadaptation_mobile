package com.uphf.saes5.data.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.uphf.saes5.testing.InMemoryPreferenceStore;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/** Notice et consentement de recherche (US-9.1). */
public class ConsentRepositoryTest {

    private final ConsentRepository consent = new ConsentRepository(
            new InMemoryPreferenceStore(), Clock.fixed(Instant.ofEpochMilli(42L), ZoneOffset.UTC));

    @Test
    public void neverAccepted_requiresConsent() {
        assertTrue(ConsentRepository.isRequired(
                ConsentRepository.NONE, ConsentRepository.CURRENT_VERSION));
        assertTrue(consent.isRequired());
    }

    @Test
    public void acceptedCurrentNotice_doesNotAskAgain() {
        assertFalse(ConsentRepository.isRequired(
                ConsentRepository.CURRENT_VERSION, ConsentRepository.CURRENT_VERSION));
    }

    @Test
    public void aNewerNotice_requiresConsentAgain() {
        // Changer ce que l'application collecte impose un nouvel accord éclairé.
        assertTrue(ConsentRepository.isRequired(1, 2));
    }

    @Test
    public void accepting_recordsTheDate_andWithdrawingAsksAgain() {
        consent.accept();

        assertFalse(consent.isRequired());
        assertEquals(42L, consent.acceptedAt());

        consent.withdraw();

        assertTrue(consent.isRequired());
        assertEquals(0L, consent.acceptedAt());
    }
}
