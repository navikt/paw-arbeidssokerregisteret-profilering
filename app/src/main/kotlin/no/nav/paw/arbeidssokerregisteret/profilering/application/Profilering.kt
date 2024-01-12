package no.nav.paw.arbeidssokerregisteret.profilering.application

import no.nav.paw.aareg.Arbeidsforhold
import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke
import no.nav.paw.arbeidssokerregisteret.api.v1.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import java.time.Duration
import java.time.LocalDate

fun profiler(
    arbeidsforhold: List<Arbeidsforhold>,
    opplysninger: OpplysningerOmArbeidssoeker
): Profilering {
    val helseHinderEllerAndreForhold = opplysninger.annet.andreForholdHindrerArbeid == JaNeiVetIkke.JA ||
            opplysninger.helse.helsetilstandHindrerArbeid == JaNeiVetIkke.JA
    TODO()
}

fun anbefalerStandardInnsats(opplysninger: OpplysningerOmArbeidssoeker, oppfylerKravTilArbeidsErfaring: Boolean): Boolean {
    TODO()
}
fun List<SammhengendeJobb>.maksSammenhengendeJobbIPeriode(periode: OpenEndRange<LocalDate>): Duration {
    TODO()
}

fun jobbTidsLinje(margin: Duration, arbeidsforhold: List<Arbeidsforhold>): List<SammhengendeJobb> =
    arbeidsforhold
        .flettSammenhengendeJobber(margin)

fun List<Arbeidsforhold>.flettSammenhengendeJobber(margin: Duration): List<SammhengendeJobb> =
    drop(1)
        .fold(listOf(somSammenhengendeJobb(first(), margin))) { acc, arbeidsforhold ->
            val match = acc.find { it.erDelAv(arbeidsforhold) }
            if (match != null) {
                acc - match + match.leggTil(arbeidsforhold)
            } else {
                acc + somSammenhengendeJobb(arbeidsforhold, margin)
            }
        }

fun somSammenhengendeJobb(arbeidsforhold: Arbeidsforhold, margin: Duration): SammhengendeJobb {
    return SammhengendeJobb(
        margin = margin,
        tid = arbeidsforhold.ansettelsesperiode.periode.fom.rangeUntil(
            arbeidsforhold.ansettelsesperiode.periode.tom ?: LocalDate.MAX
        ),
        arbeidsforhold = listOf(arbeidsforhold)
    )
}

data class SammhengendeJobb(
    val margin: Duration,
    val tid: OpenEndRange<LocalDate>,
    val arbeidsforhold: List<Arbeidsforhold>
) {
    fun erDelAv(arbeidsforhold: Arbeidsforhold): Boolean {
        return tid.contains(arbeidsforhold.ansettelsesperiode.periode.fom) ||
                tid.contains(arbeidsforhold.ansettelsesperiode.periode.tom ?: LocalDate.MAX)
    }

    fun leggTil(arbeidsforhold: Arbeidsforhold): SammhengendeJobb {
        if (!erDelAv(arbeidsforhold)) throw IllegalArgumentException("Arbeidsforholdet er ikke del av denne tidslinjen")
        val nyStart = minOf(tid.start, arbeidsforhold.ansettelsesperiode.periode.fom.minus(margin))
        val nySlutt =
            maxOf(tid.endExclusive, arbeidsforhold.ansettelsesperiode.periode.tom?.plus(margin) ?: LocalDate.MAX)
        return copy(
            tid = nyStart.rangeUntil(nySlutt),
            arbeidsforhold = this.arbeidsforhold + arbeidsforhold
        )
    }
}
