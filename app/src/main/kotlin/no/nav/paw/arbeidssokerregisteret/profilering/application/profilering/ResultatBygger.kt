package no.nav.paw.arbeidssokerregisteret.profilering.application.profilering

import no.nav.paw.arbeidssokerregisteret.api.v1.*
import no.nav.paw.arbeidssokerregisteret.profilering.utils.ApplicationInfo
import java.time.Instant
import java.util.*

fun profileringsResultat(
    alder: Int?,
    opplysninger: OpplysningerOmArbeidssoeker,
    profilering: ProfilertTil,
    harJobbetSammenhengende6AvSiste12Mnd: Boolean
): Profilering {
    return Profilering(
        UUID.randomUUID(),
        opplysninger.periodeId,
        opplysninger.id,
        Metadata(
            Instant.now(),
            bruker,
            ApplicationInfo.id,
            "opplysninger-mottatt"
        ),
        profilering,
        harJobbetSammenhengende6AvSiste12Mnd,
        alder
    )
}

val bruker = Bruker(
    BrukerType.SYSTEM,
    ApplicationInfo.name
)
