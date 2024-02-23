package no.nav.paw.arbeidssokerregisteret.profilering.application.profilering

import no.nav.paw.aareg.Arbeidsforhold
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Helse
import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke
import no.nav.paw.arbeidssokerregisteret.api.v3.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.api.v3.Utdanning
import no.nav.paw.arbeidssokerregisteret.profilering.personinfo.PersonInfo
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.*
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata as ApiMetadata

fun personInfo(
    vararg arbeidsforhold: Arbeidsforhold,
    foedselsdato: String?,
    foedselsAar: Int = requireNotNull(foedselsdato).substring(0, 4).toInt()
) = PersonInfo(
    foedselsdato = foedselsdato?.let(LocalDate::parse),
    foedselsAar = foedselsAar,
    arbeidsforhold = arbeidsforhold.toList()
)
