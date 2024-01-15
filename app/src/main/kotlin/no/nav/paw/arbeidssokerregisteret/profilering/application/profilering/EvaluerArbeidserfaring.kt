package no.nav.paw.arbeidssokerregisteret.profilering.application.profilering

import no.nav.paw.aareg.Arbeidsforhold
import no.nav.paw.arbeidssokerregisteret.profilering.application.profilering.ProfileringsTagger.OPPFYLLER_KRAV_TIL_ARBEIDSERFARING
import java.time.Duration
import java.time.LocalDate
import java.time.Period

fun evaluerArbeidsErfaring(
    marginForSammenhengendeJobb: Duration,
    minimumsArbeidserfaring: Period,
    periode: OpenEndRange<LocalDate>,
    arbeidsforhold: List<Arbeidsforhold>
): Set<ProfileringsTagger> {
    val sammenhengendeJobber = lengsteSammenhengendeInnenforPeriode(
        arbeidsforhold = arbeidsforhold,
        periode = periode,
        marginForSammenhengendeJobb = marginForSammenhengendeJobb
    )
    return when {
        sammenhengendeJobber == null -> emptySet()
        sammenhengendeJobber.tid.length().days >= minimumsArbeidserfaring.days -> setOf(
            OPPFYLLER_KRAV_TIL_ARBEIDSERFARING
        )
        else -> emptySet()
    }
}

fun lengsteSammenhengendeInnenforPeriode(
    arbeidsforhold: List<Arbeidsforhold>,
    periode: OpenEndRange<LocalDate>,
    marginForSammenhengendeJobb: Duration
): SammhengendeJobb? =
    arbeidsforhold.flettSammenhengendeJobber(marginForSammenhengendeJobb)
        .filter { sammenhengendePeriode ->
            sammenhengendePeriode.tid.contains(periode.start) ||
            sammenhengendePeriode.tid.contains(periode.endExclusive)
        }.maxByOrNull { it.periodeInnenfor(periode).length().days }

fun SammhengendeJobb.periodeInnenfor(periode: OpenEndRange<LocalDate>): OpenEndRange<LocalDate> =
    maxOf(tid.start, periode.start).rangeUntil(minOf(tid.endExclusive, periode.endExclusive))

fun OpenEndRange<LocalDate>.length(): Period = Period.between(start, endExclusive)
fun List<Arbeidsforhold>.flettSammenhengendeJobber(margin: Duration): List<SammhengendeJobb> =
    if (isEmpty()) {
        emptyList()
    } else {
        drop(1)
            .fold(listOf(somSammenhengendeJobb(first(), margin))) { acc, arbeidsforhold ->
                val match = acc.find { it.erDelAv(arbeidsforhold) }
                if (match != null) {
                    acc - match + match.leggTil(arbeidsforhold)
                } else {
                    acc + somSammenhengendeJobb(arbeidsforhold, margin)
                }
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
    private val margin: Duration,
    val tid: OpenEndRange<LocalDate>,
    val arbeidsforhold: List<Arbeidsforhold>
) {
    private val tidMedMargin get() = tid.start.minus(margin).rangeUntil(
        if (tid.endExclusive == LocalDate.MAX) LocalDate.MAX else tid.endExclusive.plus(margin)
    )
    fun erDelAv(arbeidsforhold: Arbeidsforhold): Boolean {
        return tidMedMargin.contains(arbeidsforhold.ansettelsesperiode.periode.fom) ||
                tidMedMargin.contains(arbeidsforhold.ansettelsesperiode.periode.tom ?: LocalDate.MAX)
    }

    fun leggTil(arbeidsforhold: Arbeidsforhold): SammhengendeJobb {
        if (!erDelAv(arbeidsforhold)) throw IllegalArgumentException("Arbeidsforholdet er ikke del av denne tidslinjen")
        val nyStart = minOf(
            a = tid.start,
            b = arbeidsforhold.ansettelsesperiode.periode.fom
        )
        val nySlutt = maxOf(
            a = tid.endExclusive,
            b = arbeidsforhold.ansettelsesperiode.periode.tom ?: LocalDate.MAX
        )
        return copy(
            tid = nyStart.rangeUntil(nySlutt),
            arbeidsforhold = this.arbeidsforhold + arbeidsforhold
        )
    }
}