package no.nav.paw.arbeidssokerregisteret.profilering.application.profilering

import no.nav.paw.aareg.Arbeidsforhold
import no.nav.paw.arbeidssokerregisteret.profilering.application.profilering.ProfileringsTagger.OPPFYLLER_KRAV_TIL_ARBEIDSERFARING
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

fun evaluerArbeidsErfaring(
    marginForSammenhengendeJobbDager: Int,
    minimumsArbeidserfaring: Period,
    periode: OpenEndRange<LocalDate>,
    arbeidsforhold: List<Arbeidsforhold>
): Set<ProfileringsTagger> {
    val sammenhengendeJobber = lengsteSammenhengendeInnenforPeriode(
        arbeidsforhold = arbeidsforhold,
        periode = periode,
        marginForSammenhengendeJobbDager = marginForSammenhengendeJobbDager
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
    marginForSammenhengendeJobbDager: Int
): SammhengendeJobb? =
    arbeidsforhold.flettSammenhengendeJobber(marginForSammenhengendeJobbDager)
        .filter { sammenhengendePeriode ->
            (sammenhengendePeriode.tid.start <= periode.start &&
                    sammenhengendePeriode.tid.endExclusive >= periode.start) ||
                    (sammenhengendePeriode.tid.start <= periode.endExclusive &&
                            sammenhengendePeriode.tid.endExclusive >= periode.endExclusive)
        }.maxByOrNull { it.periodeInnenfor(periode).length().days }

fun SammhengendeJobb.periodeInnenfor(periode: OpenEndRange<LocalDate>): OpenEndRange<LocalDate> =
    maxOf(tid.start, periode.start).rangeUntil(minOf(tid.endExclusive, periode.endExclusive))

fun OpenEndRange<LocalDate>.length(): Period = Period.between(start, endExclusive)
fun List<Arbeidsforhold>.flettSammenhengendeJobber(marginInDays: Int): List<SammhengendeJobb> =
    if (isEmpty()) {
        emptyList()
    } else {
        drop(1)
            .fold(listOf(somSammenhengendeJobb(first(), marginInDays))) { acc, arbeidsforhold ->
                val match = acc.find { it.erDelAv(arbeidsforhold) }
                if (match != null) {
                    acc - match + match.leggTil(arbeidsforhold)
                } else {
                    acc + somSammenhengendeJobb(arbeidsforhold, marginInDays)
                }
            }
    }

fun somSammenhengendeJobb(arbeidsforhold: Arbeidsforhold, marginInDays: Int): SammhengendeJobb {
    return SammhengendeJobb(
        marginInDays = marginInDays,
        tid = arbeidsforhold.ansettelsesperiode.periode.fom.rangeUntil(
            arbeidsforhold.ansettelsesperiode.periode.tom ?: LocalDate.MAX
        ),
        arbeidsforhold = listOf(arbeidsforhold)
    )
}

data class SammhengendeJobb(
    private val marginInDays: Int,
    val tid: OpenEndRange<LocalDate>,
    val arbeidsforhold: List<Arbeidsforhold>
) {
    private val tidMedMargin
        get() = tid.start.minus(marginInDays.toLong(), ChronoUnit.DAYS).rangeUntil(
            if (tid.endExclusive == LocalDate.MAX) LocalDate.MAX else tid.endExclusive.plus(
                marginInDays.toLong(),
                ChronoUnit.DAYS
            )
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