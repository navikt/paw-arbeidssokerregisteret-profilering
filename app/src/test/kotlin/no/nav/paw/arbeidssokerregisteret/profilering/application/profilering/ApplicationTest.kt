package no.nav.paw.arbeidssokerregisteret.profilering.application.profilering

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.arbeidssokerregisteret.api.v3.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.profilering.application.APPLICATION_CONFIG_FILE
import no.nav.paw.arbeidssokerregisteret.profilering.application.ApplicationConfiguration
import no.nav.paw.arbeidssokerregisteret.profilering.application.applicationTopology
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.TestOutputTopic
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.state.Stores
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.*
import kotlin.test.fail

class ApplicationTest : FreeSpec({
    "Enkle profilerings tester" - {
        val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfiguration>(APPLICATION_CONFIG_FILE)
        val streamsBuilder = StreamsBuilder()
            .addStateStore(
                Stores.keyValueStoreBuilder(
                    Stores.inMemoryKeyValueStore(applicationConfig.joiningStateStoreName),
                    Serdes.String(),
                    createAvroSerde()
                )
            )
        val topology = applicationTopology(
            streamBuilder = streamsBuilder,
            personInfoTjeneste = { _, _ -> ProfileringTestData.standardBrukerPersonInfo() },
            applicationConfiguration = applicationConfig,
            prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
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
            opplysningerOmArbeidssoekerTopic.pipeInput(key, ProfileringTestData.standardOpplysninger())
            val outputProfilering = profileringsTopic.readValue()
            outputProfilering.periodeId shouldBe ProfileringTestData.standardProfilering().periodeId
            outputProfilering.profilertTil shouldBe ProfilertTil.ANTATT_GODE_MULIGHETER
            verifyEmptyTopic(profileringsTopic)
        }
        "profileringen skal ikke skrives til output topic når det kun kommer opplysninger" {
            val key = 2L
            opplysningerOmArbeidssoekerTopic.pipeInput(key, ProfileringTestData.standardOpplysninger())
            verifyEmptyTopic(profileringsTopic)
        }
        "profileringen skal ikke skrives til output topic når det kun kommer periode" {
            val key = 3L
            periodeTopic.pipeInput(key, ProfileringTestData.periode)
            verifyEmptyTopic(profileringsTopic)
        }
        "to profileringer skal skrives til output topic når det kommer to opplysninger med samme periode id" {
            verifyEmptyTopic(profileringsTopic)
            val key = 4L
            periodeTopic.pipeInput(key, ProfileringTestData.periode)
            opplysningerOmArbeidssoekerTopic.pipeInput(key, ProfileringTestData.standardOpplysninger())
            opplysningerOmArbeidssoekerTopic.pipeInput(key, ProfileringTestData.standardOpplysninger())

            val (recordKey1, outputProfilering1) = profileringsTopic.readKeyValue()
            recordKey1 shouldBe key
            outputProfilering1.periodeId shouldBe ProfileringTestData.standardProfilering().periodeId
            outputProfilering1.profilertTil shouldBe ProfilertTil.ANTATT_GODE_MULIGHETER
            periodeTopic.pipeInput(key, ProfileringTestData.periode)
            opplysningerOmArbeidssoekerTopic.pipeInput(key, ProfileringTestData.standardOpplysninger())
            val (recordKey2, outputProfilering2) = profileringsTopic.readKeyValue()
            recordKey2 shouldBe key
            outputProfilering2.periodeId shouldBe ProfileringTestData.profilering.periodeId
            outputProfilering2.profilertTil shouldBe ProfilertTil.ANTATT_GODE_MULIGHETER
            verifyEmptyTopic(profileringsTopic)
        }
        "profileringen skal skrives til output topic når det kommer opplysninger før periode" {
            verifyEmptyTopic(profileringsTopic)
            val key = 5L
            opplysningerOmArbeidssoekerTopic.pipeInput(key, ProfileringTestData.standardOpplysninger())
            verifyEmptyTopic(profileringsTopic)
            periodeTopic.pipeInput(key, ProfileringTestData.periode)
            val (recordKey, outputProfilering) = profileringsTopic.readKeyValue()
            recordKey shouldBe key
            outputProfilering.periodeId shouldBe ProfileringTestData.standardProfilering().periodeId
            outputProfilering.profilertTil shouldBe ProfilertTil.ANTATT_GODE_MULIGHETER
            verifyEmptyTopic(profileringsTopic)
        }
        "Verifiserer flyt for, opplysning -> start-periode -> avslutt-periode" {
            verifyEmptyTopic(profileringsTopic)
            val key = 6L
            val startTime = Instant.now()
            val opplysninger = opplysninger(
                sendtInAv = metadata(tidspunkt = startTime)
            )
            opplysningerOmArbeidssoekerTopic.pipeInput(
                key,
                opplysninger,
                opplysninger.sendtInnAv.tidspunkt
            )
            val startPeriode = periode(
                id = opplysninger.periodeId,
                startet = metadata(tidspunkt = startTime + Duration.ofMillis(513))
            )
            periodeTopic.pipeInput(
                key,
                startPeriode,
                startPeriode.startet.tidspunkt
            )
            val avsluttPeriode = periode(
                id = startPeriode.id,
                identitetsnummer = startPeriode.identitetsnummer,
                startet = startPeriode.startet,
                avsluttet = metadata(tidspunkt = startPeriode.startet.tidspunkt + Duration.ofDays(14))
            )
            periodeTopic.pipeInput(
                key,
                avsluttPeriode,
                avsluttPeriode.avsluttet!!.tidspunkt
            )
            profileringsTopic.isEmpty shouldBe false
            val testRecord1 = profileringsTopic.readRecord()
            testRecord1.key shouldBe key
            Instant.ofEpochMilli(testRecord1.timestamp()) shouldBe startPeriode.startet.tidspunkt
            verifyEmptyTopic(profileringsTopic)
            val keyValueStore: KeyValueStore<String, TopicsJoin> = testDriver.getKeyValueStore(
                applicationConfig.joiningStateStoreName
            )
            val topicsJoin = keyValueStore["${key}:${opplysninger.periodeId}"]
            topicsJoin.shouldNotBeNull()
            topicsJoin.opplysningerOmArbeidssoeker shouldBe null
            topicsJoin.periode shouldBe avsluttPeriode
            topicsJoin.profilering shouldBe null
        }
    }
})

fun verifyEmptyTopic(profileringsTopic: TestOutputTopic<out Any, out Any>) {
    if (profileringsTopic.isEmpty) return
    val records = profileringsTopic.readKeyValuesToList()
        .map { it.key to it.value }
    fail(
        "Forventet at topic ${profileringsTopic} skulle være tom, følgende records ble funnet:\n ${
            records.toList().map { "$it\n" }
        }"
    )
}

operator fun <K, V> KeyValue<K, V>.component1(): K = key
operator fun <K, V> KeyValue<K, V>.component2(): V = value