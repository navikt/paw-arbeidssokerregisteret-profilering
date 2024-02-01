package no.nav.paw.arbeidssokerregisteret.profilering.application.profilering

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v2.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.profilering.application.APPLICATION_CONFIG_FILE
import no.nav.paw.arbeidssokerregisteret.profilering.application.ApplicationConfiguration
import no.nav.paw.arbeidssokerregisteret.profilering.application.SuppressionConfig
import no.nav.paw.arbeidssokerregisteret.profilering.application.applicationTopology
import no.nav.paw.arbeidssokerregisteret.profilering.personinfo.PersonInfo
import no.nav.paw.arbeidssokerregisteret.profilering.personinfo.PersonInfoTjeneste
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.state.Stores
import java.time.Duration

class ApplicationTest : FreeSpec({
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfiguration>(APPLICATION_CONFIG_FILE)
    val streamsBuilder = StreamsBuilder()
        .addStateStore(
            Stores.timestampedKeyValueStoreBuilder(
                Stores.persistentKeyValueStore("periodeTombstoneDelayStore"),
                Serdes.Long(),
                Serdes.String()
            )
        )

    val stateStoreName = "periodeTombstoneDelayStore"
    val topology = applicationTopology(
        streamBuilder = streamsBuilder,
        personInfoTjeneste = { _, _ -> PersonInfo(null, 1990, emptyList()) },
        applicationConfiguration = applicationConfig,
        suppressionConfig = SuppressionConfig(
            stateStoreName = stateStoreName,
            scheduleInterval = Duration.ofMinutes(1),
            scheduleType = PunctuationType.WALL_CLOCK_TIME,
            suppressFor = Duration.ofHours(1),
            suppressDurationType = SuppressionConfig.Type.ANY
        ) { _, value -> value.avsluttet != null }
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
    "en hest" {
        periodeTopic.pipeInput(5L, ProfileringTestData.periode)
        opplysningerOmArbeidssoekerTopic.pipeInput(5L, ProfileringTestData.opplysningerOmArbeidssoeker)
        val outputProfilering = profileringsTopic.readValue()
        outputProfilering shouldBe ProfileringTestData.profilering
    }
})