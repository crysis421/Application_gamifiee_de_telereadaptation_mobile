package com.uphf.saes5.ui.consent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.uphf.saes5.data.session.SessionRepository;
import com.uphf.saes5.data.settings.ConsentRepository;
import com.uphf.saes5.domain.model.SessionEntry;
import com.uphf.saes5.testing.InMemoryPreferenceStore;

import java.io.File;
import java.time.Clock;

/** Notice et consentement (US-9.1) : accepter, relire, retirer. */
public class ConsentViewModelTest {

    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();
    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    private ConsentRepository consent;
    private SessionRepository sessions;

    @Before
    public void setUp() {
        consent = new ConsentRepository(new InMemoryPreferenceStore(), Clock.systemUTC());
        sessions = new SessionRepository(new File(folder.getRoot(), "sessions.json"),
                new File(folder.getRoot(), "exports"), Clock.systemUTC());
    }

    @Test
    public void accepting_opensTheApplication() {
        ConsentViewModel viewModel = new ConsentViewModel(consent, sessions, false);
        assertFalse(viewModel.isReviewMode());

        viewModel.accept();

        assertFalse(consent.isRequired());
        assertEquals(ConsentViewModel.ConsentEvent.OPEN_HOME,
                viewModel.getEvents().getValue().getContentIfNotHandled());
    }

    @Test
    public void reviewing_isOnlyPossibleOnceAccepted() {
        assertFalse(new ConsentViewModel(consent, sessions, true).isReviewMode());

        consent.accept();

        assertTrue(new ConsentViewModel(consent, sessions, true).isReviewMode());
    }

    @Test
    public void withdrawing_erasesTheJournal_andClosesTheApplication() {
        consent.accept();
        sessions.record(new SessionEntry(1L, "base_squat", "Squat", 3, 0, 12, 60_000L,
                SessionEntry.SCORE_UNAVAILABLE, "", ""));
        ConsentViewModel viewModel = new ConsentViewModel(consent, sessions, true);

        viewModel.withdraw();

        assertEquals(0, sessions.count());
        assertTrue(consent.isRequired());
        assertEquals(ConsentViewModel.ConsentEvent.CLOSE_APP,
                viewModel.getEvents().getValue().getContentIfNotHandled());
    }
}
