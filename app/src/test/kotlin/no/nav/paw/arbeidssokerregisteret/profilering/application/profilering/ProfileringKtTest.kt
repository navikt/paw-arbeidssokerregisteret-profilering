package no.nav.paw.arbeidssokerregisteret.profilering.application.profilering

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssokerregisteret.api.v1.Annet
import no.nav.paw.arbeidssokerregisteret.api.v1.Helse
import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.arbeidssokerregisteret.api.v2.Utdanning
import java.time.LocalDate

class ProfileringKtTest : FreeSpec({
    "En med alder på 61 år skal gi ${ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING}" {
        val sekstiEnAarSiden = LocalDate.now().minusYears(61)
        profiler(
            ProfileringTestData.standardBrukerPersonInfo.copy(
                foedselsdato = sekstiEnAarSiden,
                foedselsAar = sekstiEnAarSiden.year
            ),
            opplysninger = ProfileringTestData.standardOpplysningerOmArbeidssoeker
        ).profilertTil shouldBe ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
    }
    "En med alder på 17 år skal gi ${ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING}" {
        val SyttenAarSiden = LocalDate.now().minusYears(17)
        profiler(
            ProfileringTestData.standardBrukerPersonInfo.copy(
                foedselsdato = SyttenAarSiden,
                foedselsAar = SyttenAarSiden.year
            ),
            opplysninger = ProfileringTestData.standardOpplysningerOmArbeidssoeker
        ).profilertTil shouldBe ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
    }
    "En med alder innenfor 18..59 og oppfyller krav til arbeidserfaring, utdanning, helsehinder og andre forhold blir profilert til ${ProfilertTil.ANTATT_GODE_MULIGHETER}" {
        profiler(
            ProfileringTestData.standardBrukerPersonInfo,
            ProfileringTestData.standardOpplysningerOmArbeidssoeker
        ).profilertTil shouldBe ProfilertTil.ANTATT_GODE_MULIGHETER
    }
    "En med 'helsehinder' skal gi ${ProfilertTil.OPPGITT_HINDRINGER}" {
        profiler(
            ProfileringTestData.standardBrukerPersonInfo,
            ProfileringTestData.standardOpplysningerOmArbeidssoekerBuilder()
                .setHelse(Helse(JaNeiVetIkke.JA))
                .build()
        ).profilertTil shouldBe ProfilertTil.OPPGITT_HINDRINGER
    }
    "En med 'andre forhold' skal gi ${ProfilertTil.OPPGITT_HINDRINGER}" {
        profiler(
            ProfileringTestData.standardBrukerPersonInfo,
            ProfileringTestData.standardOpplysningerOmArbeidssoekerBuilder()
                .setAnnet(Annet(JaNeiVetIkke.JA))
                .build()
        ).profilertTil shouldBe ProfilertTil.OPPGITT_HINDRINGER
    }
    "En med 'utdanning bestaatt = nei' skal gi ${ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING}" {
        profiler(
            ProfileringTestData.standardBrukerPersonInfo,
            ProfileringTestData.standardOpplysningerOmArbeidssoekerBuilder()
                .setUtdanning(Utdanning("1", JaNeiVetIkke.NEI, JaNeiVetIkke.JA))
                .build()
        ).profilertTil shouldBe ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
    }
    "En med 'utdanning godkjent = nei' skal gi ${ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING}" {
        profiler(
            ProfileringTestData.standardBrukerPersonInfo,
            ProfileringTestData.standardOpplysningerOmArbeidssoekerBuilder()
                .setUtdanning(Utdanning("1", JaNeiVetIkke.JA, JaNeiVetIkke.NEI))
                .build()
        ).profilertTil shouldBe ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
    }
    "En med 'nus = 0' skal gi ${ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING}" {
        profiler(
            ProfileringTestData.standardBrukerPersonInfo,
            ProfileringTestData.standardOpplysningerOmArbeidssoekerBuilder()
                .setUtdanning(Utdanning("0", JaNeiVetIkke.JA, JaNeiVetIkke.JA))
                .build()
        ).profilertTil shouldBe ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
    }
    "En med 'nus = 9' skal gi ${ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING}" {
        profiler(
            ProfileringTestData.standardBrukerPersonInfo,
            ProfileringTestData.standardOpplysningerOmArbeidssoekerBuilder()
                .setUtdanning(Utdanning("0", JaNeiVetIkke.JA, JaNeiVetIkke.JA))
                .build()
        ).profilertTil shouldBe ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
    }
})