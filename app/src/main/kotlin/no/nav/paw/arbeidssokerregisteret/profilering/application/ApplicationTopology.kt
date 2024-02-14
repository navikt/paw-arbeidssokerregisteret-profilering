package no.nav.paw.arbeidssokerregisteret.profilering.application

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
    applicationConfiguration: ApplicationConfiguration
): Topology {
    streamBuilder
        .stream<Long, Periode>(applicationConfiguration.periodeTopic)
        .mapValues { _, periode -> TopicsJoin(periode, null, null) }
        .saveAndForwardIfComplete(
            PeriodeStateStoreSave::class,
            applicationConfiguration.joiningStateStoreName
        ).filterProfileAndForward(
            personInfoTjeneste,
            applicationConfiguration
        )

    streamBuilder
        .stream<Long, OpplysningerOmArbeidssoeker>(applicationConfiguration.opplysningerTopic)
        .mapValues { _, opplysninger -> TopicsJoin(null, null, opplysninger) }
        .saveAndForwardIfComplete(
            OpplysningerOmArbeidssoekerStateStoreSave::class,
            applicationConfiguration.joiningStateStoreName
        ).filterProfileAndForward(
            personInfoTjeneste,
            applicationConfiguration
        )
    return streamBuilder.build()
}

fun KStream<Long, TopicsJoin>.filterProfileAndForward(
    personInfoTjeneste: PersonInfoTjeneste,
    applicationConfiguration: ApplicationConfiguration
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
        .to(applicationConfiguration.profileringTopic)
}
