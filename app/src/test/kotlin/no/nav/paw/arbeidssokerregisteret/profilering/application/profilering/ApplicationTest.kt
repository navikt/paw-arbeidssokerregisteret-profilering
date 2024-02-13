package no.nav.paw.arbeidssokerregisteret.profilering.application.profilering

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.arbeidssokerregisteret.api.v3.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.profilering.application.APPLICATION_CONFIG_FILE
import no.nav.paw.arbeidssokerregisteret.profilering.application.ApplicationConfiguration
import no.nav.paw.arbeidssokerregisteret.profilering.application.applicationTopology
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.state.Stores
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class ApplicationTest : FreeSpec({
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfiguration>(APPLICATION_CONFIG_FILE)
    val streamsBuilder = StreamsBuilder()
        .addStateStore(
            Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(applicationConfig.joiningStateStoreName),
                Serdes.String(),
                createAvroSerde()
            )
        )
    val topology = applicationTopology(
        streamBuilder = streamsBuilder,
        personInfoTjeneste = { _, _ -> ProfileringTestData.personInfo },
        applicationConfiguration = applicationConfig
    )

    val testDriver = TopologyTestDriver(topology, kafkaStreamProperties)

    val periodeSerde = createAvroSerde<Periode>()
    val opplysningerOmArbeidssoekerSerde = createAvroSerde<OpplysningerOmArbeidssoeker>()
    val profileringSerde = createAvroSerde<Profilering>()

    val periodeTopic = testDriver.createInputTopic(
        applicationConfig.periodeTopic,
        Serdes.Long().serializer(),
        periodeSerde.serializer()
    )
    val opplysningerOmArbeidssoekerTopic = testDriver.createInputTopic(
        applicationConfig.opplysningerTopic,
        Serdes.Long().serializer(),
        opplysningerOmArbeidssoekerSerde.serializer()
    )
    val profileringsTopic = testDriver.createOutputTopic(
        applicationConfig.profileringTopic,
        Serdes.Long().deserializer(),
        profileringSerde.deserializer()
    )
    "profileringen skal skrives til output topic når det kommer en periode og opplysninger om arbeidssøker" {
        periodeTopic.pipeInput(1L, ProfileringTestData.periode)
        opplysningerOmArbeidssoekerTopic.pipeInput(1L, ProfileringTestData.standardOpplysningerOmArbeidssoeker)
        val outputProfilering = profileringsTopic.readValue()
        outputProfilering.periodeId shouldBe ProfileringTestData.profilering.periodeId
        outputProfilering.profilertTil shouldBe ProfilertTil.ANTATT_GODE_MULIGHETER
    }
    "profileringen skal ikke skrives til output topic når det kommer opplysninger tidligere enn periodens start" {
        periodeTopic.pipeInput(
            2L, ProfileringTestData.periode
        )
        opplysningerOmArbeidssoekerTopic.pipeInput(
            2L, ProfileringTestData.standardOpplysningerOmArbeidssoekerBuilder()
                .setSendtInnAv(
                    Metadata(
                        LocalDate.now().minusYears(1).atStartOfDay().toInstant(ZoneOffset.UTC),
                        Bruker(BrukerType.SYSTEM, ProfileringTestData.identitetsnummer),
                        "test",
                        "test"
                    )
                ).build()
        )
        profileringsTopic.isEmpty shouldBe true
    }
    "profileringen skal ikke skrives til output topic når det kun kommer opplysninger" {
        opplysningerOmArbeidssoekerTopic.pipeInput(3L, ProfileringTestData.standardOpplysningerOmArbeidssoeker)
        profileringsTopic.isEmpty shouldBe true
    }
    "profileringen skal ikke skrives til output topic når det kun kommer periode" {
        periodeTopic.pipeInput(4L, ProfileringTestData.periode)
        profileringsTopic.isEmpty shouldBe true
    }
    "profileringen skal ikke skrives til output topic når det kommer opplysninger etter periodens slutt" {
        periodeTopic.pipeInput(
            5L, ProfileringTestData.periodeBuilder()
                .setAvsluttet(
                    Metadata(
                        Instant.now(),
                        Bruker(BrukerType.SYSTEM, ProfileringTestData.identitetsnummer),
                        "test",
                        "test"
                    )
                )
                .build()
        )
        opplysningerOmArbeidssoekerTopic.pipeInput(
            5L,
            ProfileringTestData.standardOpplysningerOmArbeidssoekerBuilder()
                .setSendtInnAv(
                    Metadata(
                        LocalDate.now().plusDays(2).atStartOfDay().toInstant(ZoneOffset.UTC),
                        Bruker(BrukerType.SYSTEM, ProfileringTestData.identitetsnummer),
                        "test",
                        "test"
                    )
                ).build()
        )
        profileringsTopic.isEmpty shouldBe true
    }
})