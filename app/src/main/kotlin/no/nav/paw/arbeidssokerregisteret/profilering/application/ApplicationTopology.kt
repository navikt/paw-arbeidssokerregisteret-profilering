package no.nav.paw.arbeidssokerregisteret.profilering.application

import no.nav.paw.arbeidssokerregisteret.api.helpers.v3.TopicsJoin
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v3.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.profilering.application.profilering.profiler
import no.nav.paw.arbeidssokerregisteret.profilering.personinfo.PersonInfoTjeneste
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.slf4j.LoggerFactory

fun applicationTopology(
    streamBuilder: StreamsBuilder,
    personInfoTjeneste: PersonInfoTjeneste,
    applicationConfiguration: ApplicationConfiguration
): Topology {
    val logger = LoggerFactory.getLogger("applicationTopology")
    streamBuilder
        .stream<Long, Periode>(applicationConfiguration.periodeTopic)
        .mapValues { _, periode -> TopicsJoin(periode, null, null) }
        .saveAndForwardIfComplete(PeriodeStateStoreSave::class, applicationConfiguration.periodeStateStoreName)

    streamBuilder
        .stream<Long, OpplysningerOmArbeidssoeker>(applicationConfiguration.opplysningerTopic)
        .mapValues { _, opplysninger -> TopicsJoin(null, null, opplysninger) }
        .saveAndForwardIfComplete(OpplysningerOmArbeidssoekerStateStoreSave::class, applicationConfiguration.opplysningerStateStoreName)
        .filter { key, topicsJoins ->
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
        .mapValues { _, (personInfo, opplysninger) ->
            profiler(personInfo, opplysninger)
        }
        .to(applicationConfiguration.profileringTopic)
    return streamBuilder.build()
}

fun TopicsJoin.isComplete() = periode != null && opplysningerOmArbeidssoeker != null