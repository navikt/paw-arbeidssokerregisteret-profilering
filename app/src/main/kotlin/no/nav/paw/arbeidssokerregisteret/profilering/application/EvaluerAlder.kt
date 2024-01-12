package no.nav.paw.arbeidssokerregisteret.profilering.application

import no.nav.paw.arbeidssokerregisteret.profilering.personinfo.PersonInfo
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId

fun evaluerAlder(
    tidspunktForInnsending: Instant,
    personInfo: PersonInfo
): Set<ProfileringsTagger> {
    val utgangspunkt = tidspunktForInnsending.atZone(ZoneId.systemDefault()).toLocalDate()
    val dato = personInfo.foedselsdato
        ?: personInfo.foedselsAar?.let { aar -> LocalDate.of(aar, 7, 1) }
    val alder = dato?.let { Period.between(it, utgangspunkt).years }
    return when {
        alder == null -> setOf(ProfileringsTagger.UKJENT_ALDER)
        alder in (18..59) -> setOf(ProfileringsTagger.ALDER_INDIKERER_STANDARD_INNSATS)
        else -> emptySet()
    }
}