package no.nav.paw.arbeidssokerregisteret.profilering.application.profilering

import io.kotest.core.spec.style.FreeSpec
import no.nav.paw.aareg.*
import no.nav.paw.aareg.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.*
import no.nav.paw.arbeidssokerregisteret.profilering.personinfo.PersonInfo
import java.time.Duration
import java.time.LocalDate
import java.time.Month
import java.time.ZoneOffset
import java.util.*

class ProfileringKtTest: FreeSpec({
    val idag = LocalDate.of(2021, Month.JUNE, 1)

    "En alder på 61 år skal trigge ${ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING}" {
        profiler(
            personInfo = PersonInfo(
                foedselsdato = LocalDate.of(1960, Month.MAY, 1),
                foedselsAar = 1960,
                arbeidsforhold = listOf(
                    Arbeidsforhold(
                        arbeidsgiver = Arbeidsgiver(
                            type = "Arbeidsgiver",
                            organisasjonsnummer = "123456789"
                        ),
                        ansettelsesperiode = Ansettelsesperiode(
                            periode = Periode(
                                fom = LocalDate.of(2018, Month.JANUARY, 1),
                                tom = idag.minus(Duration.ofDays(2))
                            )
                        ),
                        opplysningspliktig = Opplysningspliktig(
                            type = "",
                            organisasjonsnummer = "123456789"
                        ),
                        arbeidsavtaler = emptyList(),
                        registrert = idag.minus(Duration.ofDays(1)).atStartOfDay()
                    )
                )
            ),
            opplysninger = OpplysningerOmArbeidssoeker(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Metadata(
                    idag.atStartOfDay().toInstant(ZoneOffset.UTC),
                    Bruker(
                        BrukerType.SYSTEM,
                        "junit"
                    ),
                    "junit",
                    "unit-test"
                ),
                Utdanning(
                    Utdanningsnivaa.GRUNNSKOLE,
                    JaNeiVetIkke.JA,
                    JaNeiVetIkke.JA,
                ),
                Helse(JaNeiVetIkke.NEI),
                Arbeidserfaring(JaNeiVetIkke.JA),
                Jobbsituasjon(emptyList()),
                Annet(JaNeiVetIkke.NEI)
            )
        )
    }
})