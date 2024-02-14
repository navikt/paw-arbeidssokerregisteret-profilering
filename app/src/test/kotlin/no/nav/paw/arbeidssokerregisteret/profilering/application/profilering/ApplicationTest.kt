package no.nav.paw.arbeidssokerregisteret.profilering.application.profilering

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssokerregisteret.api.helpers.v3.TopicsJoin
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.arbeidssokerregisteret.api.v3.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.profilering.application.APPLICATION_CONFIG_FILE
import no.nav.paw.arbeidssokerregisteret.profilering.application.ApplicationConfiguration
import no.nav.paw.arbeidssokerregisteret.profilering.application.applicationTopology
import no.nav.paw.arbeidssokerregisteret.profilering.application.compositeKey
import no.nav.paw.arbeidssokerregisteret.profilering.application.profilering.ProfileringTestData.toInstant
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.state.Stores
import java.time.Instant
import java.time.LocalDate
import java.util.*

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
        val key = 1L
        periodeTopic.pipeInput(key, ProfileringTestData.periode)
        opplysningerOmArbeidssoekerTopic.pipeInput(key, ProfileringTestData.standardOpplysningerOmArbeidssoeker)
        val outputProfilering = profileringsTopic.readValue()
        outputProfilering.periodeId shouldBe ProfileringTestData.profilering.periodeId
        outputProfilering.profilertTil shouldBe ProfilertTil.ANTATT_GODE_MULIGHETER
    }
    "profileringen skal ikke skrives til output topic når det kun kommer opplysninger" {
        val key = 2L
        opplysningerOmArbeidssoekerTopic.pipeInput(key, ProfileringTestData.standardOpplysningerOmArbeidssoeker)
        profileringsTopic.isEmpty shouldBe true
    }
    "profileringen skal ikke skrives til output topic når det kun kommer periode" {
        val key = 3L
        periodeTopic.pipeInput(key, ProfileringTestData.periode)
        profileringsTopic.isEmpty shouldBe true
    }
    "to profileringer skal skrives til output topic når det kommer to opplysninger med samme periode id" {
        val key = 4L
        periodeTopic.pipeInput(key, ProfileringTestData.periode)
        opplysningerOmArbeidssoekerTopic.pipeInput(key, ProfileringTestData.standardOpplysningerOmArbeidssoeker)
        opplysningerOmArbeidssoekerTopic.pipeInput(key, ProfileringTestData.standardOpplysningerOmArbeidssoeker)

        val outputProfilering1 = profileringsTopic.readValue()
        outputProfilering1.periodeId shouldBe ProfileringTestData.profilering.periodeId
        outputProfilering1.profilertTil shouldBe ProfilertTil.ANTATT_GODE_MULIGHETER
        periodeTopic.pipeInput(key, ProfileringTestData.periode)
        opplysningerOmArbeidssoekerTopic.pipeInput(key, ProfileringTestData.standardOpplysningerOmArbeidssoeker)
        val outputProfilering2 = profileringsTopic.readValue()
        outputProfilering2.periodeId shouldBe ProfileringTestData.profilering.periodeId
        outputProfilering2.profilertTil shouldBe ProfilertTil.ANTATT_GODE_MULIGHETER
    }
    "profileringen skal skrives til output topic når det kommer opplysninger før periode" {
        val key = 5L
        opplysningerOmArbeidssoekerTopic.pipeInput(key, ProfileringTestData.standardOpplysningerOmArbeidssoeker)
        periodeTopic.pipeInput(key, ProfileringTestData.periode)
        val outputProfilering = profileringsTopic.readValue()
        outputProfilering.periodeId shouldBe ProfileringTestData.profilering.periodeId
        outputProfilering.profilertTil shouldBe ProfilertTil.ANTATT_GODE_MULIGHETER
    }
    /*
    "scheduleCleanup skal slette utdaterte records fra state store" {
        val key = 6L
        periodeTopic.pipeInput(key, ProfileringTestData.avsluttetPeriode, Instant.now())
        val compositeKey = compositeKey(key, ProfileringTestData.avsluttetPeriode.id)

        testDriver
            .getKeyValueStore<String, TopicsJoin>(applicationConfig.joiningStateStoreName)
            .get(compositeKey) shouldBe null

        opplysningerOmArbeidssoekerTopic.pipeInput(key, ProfileringTestData.standardOpplysningerOmArbeidssoeker)

        profileringsTopic.isEmpty shouldBe true
    }
    "scheduleCleanup skal slette dinglende opplysninger fra state store" {
        val key = 7L
        opplysningerOmArbeidssoekerTopic.pipeInput(key, ProfileringTestData
            .standardOpplysninger(LocalDate.now().minusYears(1).toInstant())
        )

        periodeTopic.pipeInput(key, ProfileringTestData.periode, Instant.now())

        profileringsTopic.isEmpty shouldBe true
    }
    */
    "når opplysninger kommer først, og perioden etterpå, skal det profileres" {
        val key = 8L
        opplysningerOmArbeidssoekerTopic.pipeInput(key, ProfileringTestData
            .standardOpplysninger(Instant.now())
        )

        periodeTopic.pipeInput(key, ProfileringTestData.periode, Instant.now())

        profileringsTopic.isEmpty shouldBe false
    }
})