package no.nav.paw.arbeidssokerregisteret.profilering.application

import no.nav.paw.arbeidssokerregisteret.api.v1.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Suppressed
import java.time.Duration

fun applicationTopology(
    suppressionConfig: SuppressionConfig<Long, Periode>,
    streamBuilder: StreamsBuilder,
    arbeidsforholdTjeneste: ArbeidsforholdTjeneste,
    applicationConfiguration: ApplicationConfiguration
): Topology {
    val periodeTabell = streamBuilder
        .stream<Long, Periode>(applicationConfiguration.periodeTopic)
        .conditionallySuppress(suppressionConfig)
        .mapValues { _, periode -> if (periode.avsluttet != null) periode else null }
        .toTable()


    val opplysninger = streamBuilder
        .stream<Long, OpplysningerOmArbeidssoeker>(applicationConfiguration.opplysningerTopic)

    opplysninger.join(periodeTabell) { opplysninger, periode ->
        periode?.identitetsnummer?.let { identitetsnummer ->
            identitetsnummer to opplysninger
        }
    }.mapValues { _, (identitetsnummer, opplysninger) ->
        val arbeidsforhold = arbeidsforholdTjeneste.arbeidsforhold(identitetsnummer, opplysninger.id)
        arbeidsforhold to opplysninger
    }.mapValues { _, (arbeidsforhold, opplysninger) ->
        profiler(arbeidsforhold, opplysninger)
    }
    return streamBuilder.build()
}

