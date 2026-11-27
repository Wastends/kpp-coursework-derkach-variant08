package ua.edu.nupp.kpp.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static ua.edu.nupp.kpp.server.SampleState.*;

class SampleStateTest {

    @Test void registeredToValidated() {
        assertTrue(WaterSampleService.isAllowedTransition(REGISTERED, VALIDATED));
    }

    @Test void validatedToAnalyzed() {
        assertTrue(WaterSampleService.isAllowedTransition(VALIDATED, ANALYZED));
    }

    @Test void analyzedToArchived() {
        assertTrue(WaterSampleService.isAllowedTransition(ANALYZED, ARCHIVED));
    }

    @Test void registeredToArchivedRejected() {
        assertFalse(WaterSampleService.isAllowedTransition(REGISTERED, ARCHIVED));
    }

    @Test void reverseTransitionRejected() {
        assertFalse(WaterSampleService.isAllowedTransition(ANALYZED, VALIDATED));
    }

    @Test void archivedIsTerminal() {
        for (SampleState next : values()) {
            assertFalse(WaterSampleService.isAllowedTransition(ARCHIVED, next));
        }
    }

    @Test void missingStateRejected() {
        assertFalse(WaterSampleService.isAllowedTransition(null, VALIDATED));
        assertFalse(WaterSampleService.isAllowedTransition(REGISTERED, null));
    }
}
