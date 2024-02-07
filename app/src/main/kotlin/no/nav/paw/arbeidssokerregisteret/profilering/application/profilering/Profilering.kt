package no.nav.paw.arbeidssokerregisteret.profilering.application.profilering

import no.nav.paw.arbeidssokerregisteret.api.v3.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.arbeidssokerregisteret.profilering.application.profilering.ProfileringsTagger.*
import no.nav.paw.arbeidssokerregisteret.profilering.personinfo.PersonInfo
import java.time.Period
import java.time.ZoneId

fun profiler(
    personInfo: PersonInfo,
    opplysninger: OpplysningerOmArbeidssoeker
): Profilering {
    val sendtInnTidspunkt = opplysninger.sendtInnAv.tidspunkt
    val sendtInnDato = sendtInnTidspunkt.atZone(ZoneId.systemDefault()).toLocalDate()
    val alder = alderVedTidspunkt(sendtInnTidspunkt, personInfo)
    val evalueringer =
        evaluerAnnet(opplysninger.annet) +
                evaluerAlder(alder) +
                evaluerHelse(opplysninger.helse) +
                evaluerUtdanning(opplysninger.utdanning) +
                evaluerArbeidsErfaring(
                    3,
                    Period.ofMonths(6),
                    sendtInnDato.minusYears(1)..<sendtInnDato,
                    personInfo.arbeidsforhold
                )

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

