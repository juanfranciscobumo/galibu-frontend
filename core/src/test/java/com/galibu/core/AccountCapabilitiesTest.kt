package com.galibu.core

import com.galibu.core.data.model.UserProfile
import com.galibu.core.data.model.canHire
import com.galibu.core.data.model.canOffer
import com.galibu.core.data.model.hasCapability
import com.galibu.core.data.model.hasMissingForMode
import com.galibu.core.data.model.preferredMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fase 4.3: capacidades de la cuenta, modo inicial con clamp y gates de onboarding.
 */
class AccountCapabilitiesTest {

    private fun profile(role: String, department: String? = "Bogotá D.C.", municipality: String? = "Bogotá", address: String? = "Calle 1", workCategory: String? = "Plomería") =
        UserProfile(
            id = "u1",
            name = "Ana",
            email = "ana@test.com",
            role = role,
            department = department,
            municipality = municipality,
            address = address,
            workCategory = workCategory
        )

    @Test
    fun clientCanHireButNotOffer() {
        val user = profile("CLIENT")
        assertTrue(user.canHire())
        assertFalse(user.canOffer())
        assertTrue(user.hasCapability("CLIENT"))
        assertFalse(user.hasCapability("PROFESSIONAL"))
    }

    @Test
    fun professionalCanOfferButNotHire() {
        val user = profile("PROFESSIONAL")
        assertFalse(user.canHire())
        assertTrue(user.canOffer())
        assertTrue(user.hasCapability("PROFESSIONAL"))
        assertFalse(user.hasCapability("CLIENT"))
    }

    @Test
    fun bothCanHireAndOffer() {
        val user = profile("BOTH")
        assertTrue(user.canHire())
        assertTrue(user.canOffer())
        assertTrue(user.hasCapability("CLIENT"))
        assertTrue(user.hasCapability("PROFESSIONAL"))
    }

    @Test
    fun preferredModeRespectsValidPersistedMode() {
        assertEquals("PROFESSIONAL", profile("BOTH").preferredMode("PROFESSIONAL"))
        assertEquals("CLIENT", profile("CLIENT").preferredMode("CLIENT"))
        assertEquals("PROFESSIONAL", profile("PROFESSIONAL").preferredMode("PROFESSIONAL"))
    }

    @Test
    fun preferredModeFallsBackToClientForInvalidPersistedMode() {
        // CLIENT sin capacidad profesional: el last_mode inválido se corrige a CLIENT.
        assertEquals("CLIENT", profile("CLIENT").preferredMode("PROFESSIONAL"))
        // Sin persistencia: CLIENT es el default seguro.
        assertEquals("CLIENT", profile("BOTH").preferredMode(null))
        // Profesional puro sin persistencia: su única capacidad.
        assertEquals("PROFESSIONAL", profile("PROFESSIONAL").preferredMode(null))
        // Profesional puro con last_mode CLIENT: se corrige a profesional.
        assertEquals("PROFESSIONAL", profile("PROFESSIONAL").preferredMode("CLIENT"))
    }

    @Test
    fun nullUserDefaultsToClientModeButMissingFields() {
        val user: UserProfile? = null
        assertEquals("CLIENT", user.preferredMode("PROFESSIONAL"))
        assertTrue(user.hasMissingForMode("CLIENT"))
        assertTrue(user.hasMissingForMode("PROFESSIONAL"))
        assertFalse(user.hasCapability("CLIENT"))
    }

    @Test
    fun clientGateRequiresBasicFieldsOnly() {
        assertFalse(profile("CLIENT").hasMissingForMode("CLIENT"))
        assertTrue(profile("CLIENT", department = null).hasMissingForMode("CLIENT"))
        assertTrue(profile("CLIENT", municipality = "").hasMissingForMode("CLIENT"))
        assertTrue(profile("CLIENT", address = "").hasMissingForMode("CLIENT"))
    }

    @Test
    fun professionalGateAlsoRequiresCategory() {
        assertFalse(profile("PROFESSIONAL").hasMissingForMode("PROFESSIONAL"))
        assertTrue(profile("BOTH", workCategory = null).hasMissingForMode("PROFESSIONAL"))
        // El mismo perfil completo no bloquea el modo cliente.
        assertFalse(profile("BOTH", workCategory = null).hasMissingForMode("CLIENT"))
    }
}
