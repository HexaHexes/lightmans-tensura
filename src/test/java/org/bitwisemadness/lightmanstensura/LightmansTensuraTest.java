package org.bitwisemadness.lightmanstensura;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Keeps the junit CI check non-vacuous: the suite must always assert something
 * real, so the check cannot pass by running zero tests.
 */
class LightmansTensuraTest {
    @Test
    void modIdIsPlainLowercaseIdentifier() {
        assertTrue(LightmansTensura.MODID.matches("[a-z][a-z0-9_]*"),
                "MODID must be a plain lowercase identifier, got: " + LightmansTensura.MODID);
    }
}
