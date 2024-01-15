package no.nav.paw.arbeidssokerregisteret.profilering.application.profilering

import no.nav.paw.arbeidssokerregisteret.api.v1.Annet
import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke.NEI
import no.nav.paw.arbeidssokerregisteret.profilering.application.profilering.ProfileringsTagger.ANDRE_FORHOLD_HINDRER_ARBEID

fun evaluerAnnet(annet: Annet): Set<ProfileringsTagger> {
    return if (annet.andreForholdHindrerArbeid == NEI) {
        emptySet()
    } else {
        setOf(ANDRE_FORHOLD_HINDRER_ARBEID)
    }
}