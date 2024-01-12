package no.nav.paw.arbeidssokerregisteret.profilering.application

import no.nav.paw.arbeidssokerregisteret.api.v1.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.profilering.personinfo.PersonInfoTjeneste
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology

fun applicationTopology(
    suppressionConfig: SuppressionConfig<Long, Periode>,
    streamBuilder: StreamsBuilder,
    personInfoTjeneste: PersonInfoTjeneste,
    applicationConfiguration: ApplicationConfiguration
): Topology {
    val periodeTabell = streamBuilder
        .stream<Long, Periode>(applicationConfiguration.periodeTopic)
        .conditionallySuppress(suppressionConfig)
        .mapValues { _, periode -> if (periode.avsluttet != null) periode else null }
        .toTable()

    streamBuilder
        .stream<Long, OpplysningerOmArbeidssoeker>(applicationConfiguration.opplysningerTopic)
        .join(periodeTabell) { opplysninger, periode ->
            periode?.identitetsnummer?.let { identitetsnummer ->
                identitetsnummer to opplysninger
            }
        }.mapValues { _, (identitetsnummer, opplysninger) ->
            val personInfo = personInfoTjeneste.hentPersonInfo(identitetsnummer, opplysninger.id)
            personInfo to opplysninger
        }.mapValues { _, (personInfo, opplysninger) ->
            profiler(personInfo, opplysninger)
        }
    return streamBuilder.build()
}

