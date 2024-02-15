package no.nav.paw.arbeidssokerregisteret.profilering.application

import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.api.helpers.v3.TopicsJoin
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v3.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.profilering.application.profilering.profiler
import no.nav.paw.arbeidssokerregisteret.profilering.personinfo.PersonInfoTjeneste
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.KStream
import org.slf4j.LoggerFactory


private val logger = LoggerFactory.getLogger("applicationTopology")

fun applicationTopology(
    streamBuilder: StreamsBuilder,
    personInfoTjeneste: PersonInfoTjeneste,
    applicationConfiguration: ApplicationConfiguration,
    prometheusRegistry: PrometheusMeterRegistry
): Topology {
    streamBuilder
        .stream<Long, Periode>(applicationConfiguration.periodeTopic)
        .mapValues { _, periode -> TopicsJoin(periode, null, null) }
        .saveAndForwardIfComplete(
            PeriodeStateStoreSave::class,
            applicationConfiguration.joiningStateStoreName,
            prometheusRegistry
        ).filterProfileAndForward(
            personInfoTjeneste,
            applicationConfiguration,
            prometheusRegistry
        )

    streamBuilder
        .stream<Long, OpplysningerOmArbeidssoeker>(applicationConfiguration.opplysningerTopic)
        .mapValues { _, opplysninger -> TopicsJoin(null, null, opplysninger) }
        .saveAndForwardIfComplete(
            OpplysningerOmArbeidssoekerStateStoreSave::class,
            applicationConfiguration.joiningStateStoreName,
            prometheusRegistry
        ).filterProfileAndForward(
            personInfoTjeneste,
            applicationConfiguration,
            prometheusRegistry,
        )
    return streamBuilder.build()
}

fun KStream<Long, TopicsJoin>.filterProfileAndForward(
    personInfoTjeneste: PersonInfoTjeneste,
    applicationConfiguration: ApplicationConfiguration,
    prometheusRegistry: PrometheusMeterRegistry
) {
    filter { key, topicsJoins ->
        topicsJoins.isComplete().also { complete ->
            if (!complete) {
                logger.error(
                    "Mangler enten periode eller opplysninger om arbeidssøker, denne skulle ikke vært videresendt: key={}",
                    key
                )
            }
        }
    }.mapValues { _, topicsJoins ->
        val periode = topicsJoins.periode
        val opplysninger = topicsJoins.opplysningerOmArbeidssoeker
        val personInfo = personInfoTjeneste.hentPersonInfo(periode.identitetsnummer, opplysninger.id)
        personInfo to opplysninger
    }
        .mapValues { _, (personInfo, opplysninger) -> profiler(personInfo, opplysninger) }
        .peek { _, profilering ->
            prometheusRegistry.counter(
                METRICS_PROFILERING,
                Tags.of(
                    Tag.of(LABEL_PROFILERT_TIL, profilering.profilertTil.name)
                )
            ).increment()
        }
        .to(applicationConfiguration.profileringTopic)
}
