package no.nav.paw.arbeidssokerregisteret.profilering.application.profilering

import no.nav.paw.arbeidssokerregisteret.api.v1.*
import no.nav.paw.arbeidssokerregisteret.profilering.application.profilering.ProfileringsTagger.*
import no.nav.paw.arbeidssokerregisteret.profilering.personinfo.PersonInfo
import no.nav.paw.arbeidssokerregisteret.profilering.utils.ApplicationInfo
import java.time.Instant
import java.util.*

fun profiler(
    personInfo: PersonInfo,
    opplysninger: OpplysningerOmArbeidssoeker
): Profilering {
    val alder = alderVedTidspunkt(opplysninger.sendtInnAv.tidspunkt, personInfo)
    val evalueringer =
        evaluerAnnet(opplysninger.annet) +
                evaluerAlder(alder) +
                evaluerHelse(opplysninger.helse) +
                evaluerUtdanning(opplysninger.utdanning)

    fun resultat(profilering: ProfilertTil) =
        profileringsResultat(
            alder = alder,
            opplysninger = opplysninger,
            profilering = profilering,
            harJobbetSammenhengende6AvSiste12Mnd = evalueringer.contains(OPPFYLLER_KRAV_TIL_ARBEIDSERFARING)
        )
    return when {
        evalueringer.anbefalerArbeidsevnevudering() -> resultat(ProfilertTil.OPPGITT_HINDRINGER)
        evalueringer.anbefalerStandardInnsats() -> resultat(ProfilertTil.ANTATT_GODE_MULIGHETER)
        else -> resultat(ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING)

    }
}

fun Set<ProfileringsTagger>.anbefalerStandardInnsats(): Boolean =
    contains(ALDER_INDIKERER_STANDARD_INNSATS) &&
            contains(OPPFYLLER_KRAV_TIL_ARBEIDSERFARING) &&
            contains(HAR_BESTAATT_GODKJENT_UTDANNING) &&
            !contains(HELSETILSTAND_HINDRER_ARBEID) &&
            !contains(ANDRE_FORHOLD_HINDRER_ARBEID)



fun Set<ProfileringsTagger>.anbefalerArbeidsevnevudering(): Boolean =
    contains(HELSETILSTAND_HINDRER_ARBEID) ||
            contains(ANDRE_FORHOLD_HINDRER_ARBEID)

