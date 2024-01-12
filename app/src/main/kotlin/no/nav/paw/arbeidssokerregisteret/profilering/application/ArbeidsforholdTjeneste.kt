package no.nav.paw.arbeidssokerregisteret.profilering.application

import no.nav.paw.aareg.Arbeidsforhold
import java.util.*

fun interface ArbeidsforholdTjeneste {
    fun arbeidsforhold(identitetsnummer: String, opplysningsId: UUID): List<Arbeidsforhold>
}