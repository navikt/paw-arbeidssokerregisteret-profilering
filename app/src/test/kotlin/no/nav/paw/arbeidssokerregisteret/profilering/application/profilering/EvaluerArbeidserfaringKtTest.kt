package no.nav.paw.arbeidssokerregisteret.profilering.application.profilering

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.aareg.Ansettelsesperiode
import no.nav.paw.aareg.Periode
import java.time.LocalDate
import java.time.Period

class EvaluerArbeidserfaringKtTest : StringSpec({

    "evaluerArbeidsErfaring skal returnere ${ProfileringsTagger.OPPFYLLER_KRAV_TIL_ARBEIDSERFARING} hvis kriteriene er møtt" {
        val marginForSammenhengendeJobb = 3
        val minimumsArbeidserfaring = Period.ofMonths(6)
        val periode = LocalDate.now().minusMonths(12)..<LocalDate.now()
        val arbeidsforhold = ProfileringTestData.arbeidsforhold

        val result =
            evaluerArbeidsErfaring(
                marginForSammenhengendeJobb,
                minimumsArbeidserfaring,
                periode,
                listOf(arbeidsforhold)
            )

        result shouldBe setOf(ProfileringsTagger.OPPFYLLER_KRAV_TIL_ARBEIDSERFARING)
    }

    "evaluerArbeidsErfaring skal returnere ${setOf(ProfileringsTagger.OPPFYLLER_KRAV_TIL_ARBEIDSERFARING)} når vi har en " +
            "jobbperiode som er lang nok og innenfor perioden" {
        val marginForSammenhengendeJobb = 3
        val minimumsArbeidserfaring = Period.ofMonths(6)
        val periode = LocalDate.now().minusMonths(12)..<LocalDate.now()
        val arbeidsforhold = ProfileringTestData.arbeidsforhold.copy(
            ansettelsesperiode = Ansettelsesperiode(
                periode = Periode(
                    fom = LocalDate.now().minusMonths(7),
                    tom = LocalDate.now()
                )
            ),
        )

        val result =
            evaluerArbeidsErfaring(
                marginForSammenhengendeJobb,
                minimumsArbeidserfaring,
                periode,
                listOf(arbeidsforhold)
            )

        result shouldBe setOf(ProfileringsTagger.OPPFYLLER_KRAV_TIL_ARBEIDSERFARING)
    }

    "evaluerArbeidsErfaring skal slå sammen to arbeidsforhold hvis de er innenfor marginen" {
        val marginForSammenhengendeJobb = 3
        val minimumsArbeidserfaring = Period.ofMonths(6)
        val periode = LocalDate.now().minusMonths(12)..<LocalDate.now()
        val arbeidsforhold1 = ProfileringTestData.arbeidsforhold.copy(
            ansettelsesperiode = Ansettelsesperiode(
                periode = Periode(
                    fom = LocalDate.now().minusMonths(12),
                    tom = LocalDate.now().minusMonths(7)
                )
            )
        )
        val arbeidsforhold2 = ProfileringTestData.arbeidsforhold.copy(
            ansettelsesperiode = Ansettelsesperiode(
                periode = Periode(
                    fom = LocalDate.now().minusMonths(7).plusDays(1),
                    tom = LocalDate.now().minusMonths(3)
                )
            )
        )

        val result =
            evaluerArbeidsErfaring(
                marginForSammenhengendeJobb,
                minimumsArbeidserfaring,
                periode,
                listOf(arbeidsforhold1, arbeidsforhold2)
            )

        result shouldBe setOf(ProfileringsTagger.OPPFYLLER_KRAV_TIL_ARBEIDSERFARING)
    }
})