package no.nav.paw.arbeidssokerregisteret.profilering.application.profilering

import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke.JA
import no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning

fun evaluerUtdanning(utdanning: Utdanning): Set<ProfileringsTagger> {
    return when {
        utdanning.nus in setOf("9", "0") -> emptySet()
        utdanning.godkjent == JA && utdanning.bestaatt == JA -> setOf(ProfileringsTagger.HAR_BESTAATT_GODKJENT_UTDANNING)
        else -> emptySet()
    }
}