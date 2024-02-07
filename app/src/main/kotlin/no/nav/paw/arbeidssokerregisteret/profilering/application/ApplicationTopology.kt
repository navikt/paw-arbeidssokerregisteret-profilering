package no.nav.paw.arbeidssokerregisteret.profilering.application

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v3.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.profilering.application.profilering.profiler
import no.nav.paw.arbeidssokerregisteret.profilering.personinfo.PersonInfoTjeneste
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.slf4j.LoggerFactory

fun applicationTopology(
    suppressionConfig: SuppressionConfig<Long, Periode>,
    streamBuilder: StreamsBuilder,
    personInfoTjeneste: PersonInfoTjeneste,
    applicationConfiguration: ApplicationConfiguration
): Topology {
    val logger = LoggerFactory.getLogger("applicationTopology")
    val periodeTabell = streamBuilder
        .stream<Long, Periode>(applicationConfiguration.periodeTopic)
        .conditionallySuppress(suppressionConfig)
        .mapValues { _, periode -> if (periode.avsluttet == null) periode else null }
        .toTable()

    streamBuilder
        .stream<Long, OpplysningerOmArbeidssoeker>(applicationConfiguration.opplysningerTopic)
        .peek { _, opplysninger -> logger.info("Opplysninger id (prejoin): ${opplysninger.id}") }
        .join(periodeTabell) { opplysninger, periode ->
            periode?.identitetsnummer?.let { identitetsnummer ->
                identitetsnummer to opplysninger
            }
        }
        .peek { _, v -> logger.info("Opplysninger id (postjoin): ${v.second.id}") }
        .mapValues { _, (identitetsnummer, opplysninger) ->
            val personInfo = personInfoTjeneste.hentPersonInfo(identitetsnummer, opplysninger.id)
            personInfo to opplysninger
        }.mapValues { _, (personInfo, opplysninger) ->
            profiler(personInfo, opplysninger)
        }.to(applicationConfiguration.profileringTopic)
    return streamBuilder.build()
}

