package no.nav.paw.arbeidssokerregisteret.profilering.application.profilering

import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke.JA
import no.nav.paw.arbeidssokerregisteret.api.v1.Utdanning
import no.nav.paw.arbeidssokerregisteret.api.v1.Utdanningsnivaa.*

fun evaluerUtdanning(utdanning: Utdanning): Set<ProfileringsTagger> {
    return if (utdanning.lengde in setOf(INGEN_UTDANNING, UDEFINERT, UKJENT_VERDI)) {
        emptySet()
    } else {
        if (utdanning.godkjent == JA && utdanning.bestaatt == JA) {
            setOf(ProfileringsTagger.HAR_BESTAATT_GODKJENT_UTDANNING)
        } else {
            emptySet()
        }
    }
}