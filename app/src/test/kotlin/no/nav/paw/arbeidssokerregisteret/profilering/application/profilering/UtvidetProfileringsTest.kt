package no.nav.paw.arbeidssokerregisteret.profilering.application.profilering

import io.kotest.core.spec.style.FreeSpec
import io.kotest.data.Row6
import io.kotest.data.row
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.arbeidssokerregisteret.api.v3.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.profilering.application.profilering.ProfileringTestData.profilering
import no.nav.paw.arbeidssokerregisteret.profilering.personinfo.PersonInfo
import java.time.ZoneId

class UtvidetProfileringsTest : FreeSpec({
    "Profilering" - {
        listOf(
            test(
                "Er 30 år og har jobbet sammenhengende i 6 av de siste 12 månedene",
                personInfo(
                    arbeidsforhold(fra = "2020-01-01", til = "2020-03-15"),
                    arbeidsforhold(fra = "2020-03-15", til = "2020-04-01"),
                    arbeidsforhold(fra = "2020-04-02", til = "2020-07-01"),
                    foedselsdato = "1990-02-27"
                ),
                opplysninger(
                    sendtIn = "2020-07-02",
                    utdanning = utdanning(
                        bestaatt = JaNeiVetIkke.JA,
                        godkjent = JaNeiVetIkke.JA
                    ),
                    helseHindrerArbeid = JaNeiVetIkke.NEI,
                    annetHindrerArbeid = JaNeiVetIkke.NEI
                ),
                alder = 30,
                harJobbetSammenhengendeSiste12Mnd = true,
                profilertTil = ProfilertTil.ANTATT_GODE_MULIGHETER
            ),
            test(
                "Er 30 år og har ikke jobbet sammenhengende i 6 av de siste 12 månedene",
                personInfo(
                    arbeidsforhold(fra = "2018-01-01", til = "2019-08-15"),
                    arbeidsforhold(fra = "2020-03-15", til = "2020-04-01"),
                    arbeidsforhold(fra = "2020-04-02", til = "2020-07-01"),
                    foedselsdato = "1990-02-27"
                ),
                opplysninger(
                    sendtIn = "2020-07-02",
                    utdanning = utdanning(
                        bestaatt = JaNeiVetIkke.JA,
                        godkjent = JaNeiVetIkke.JA
                    ),
                    helseHindrerArbeid = JaNeiVetIkke.NEI,
                    annetHindrerArbeid = JaNeiVetIkke.NEI
                ),
                alder = 30,
                harJobbetSammenhengendeSiste12Mnd = false,
                profilertTil = ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
            ),
            test("Er 62 år og har jobbet sammenhende i 6 av de 12 siste månedene",
                personInfo(
                    arbeidsforhold(fra = "2020-01-01", til = "2020-03-15"),
                    arbeidsforhold(fra = "2020-03-15", til = "2020-04-01"),
                    arbeidsforhold(fra = "2020-04-02", til = "2020-07-01"),
                    foedselsdato = "1958-02-27"
                ),
                opplysninger(
                    sendtIn = "2020-07-02",
                    utdanning = utdanning(
                        bestaatt = JaNeiVetIkke.JA,
                        godkjent = JaNeiVetIkke.JA
                    ),
                    helseHindrerArbeid = JaNeiVetIkke.NEI,
                    annetHindrerArbeid = JaNeiVetIkke.NEI
                ),
                alder = 62,
                harJobbetSammenhengendeSiste12Mnd = true,
                profilertTil = ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
            )
        ).map { (beskrivelse, personInfo, opplysninger, alder, harJobbetSammenhengendeSiste12Mnd, profilertTil) ->
            beskrivelse {
                val profilering = profiler(
                    personInfo = personInfo,
                    opplysninger = opplysninger
                )
                profilering.profilertTil shouldBe profilertTil
                profilering.alder shouldBe alder
                profilering.jobbetSammenhengendeSeksAvTolvSisteMnd shouldBe harJobbetSammenhengendeSiste12Mnd
            }
        }
    }
})


fun test(
    beskrivelse: String,
    personInfo: PersonInfo,
    opplysninger: OpplysningerOmArbeidssoeker,
    alder: Int,
    harJobbetSammenhengendeSiste12Mnd: Boolean,
    profilertTil: ProfilertTil
): Row6<String, PersonInfo, OpplysningerOmArbeidssoeker, Int, Boolean, ProfilertTil> =
    row(beskrivelse, personInfo, opplysninger, alder, harJobbetSammenhengendeSiste12Mnd, profilertTil)