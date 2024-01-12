package no.nav.paw.arbeidssokerregisteret.profilering.personinfo

import no.nav.paw.aareg.Arbeidsforhold
import java.time.LocalDate

data class PersonInfo(
    val foedselsdato: LocalDate?,
    val foedselsAar: Int?,
    val arbeidsforhold: List<Arbeidsforhold>
)