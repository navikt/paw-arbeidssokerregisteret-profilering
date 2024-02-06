package no.nav.paw.arbeidssokerregisteret.profilering

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.micrometer.core.instrument.binder.kafka.KafkaStreamsMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.profilering.application.APPLICATION_CONFIG_FILE
import no.nav.paw.arbeidssokerregisteret.profilering.application.ApplicationConfiguration
import no.nav.paw.arbeidssokerregisteret.profilering.application.SuppressionConfig
import no.nav.paw.arbeidssokerregisteret.profilering.application.applicationTopology
import no.nav.paw.arbeidssokerregisteret.profilering.health.Health
import no.nav.paw.arbeidssokerregisteret.profilering.health.initKtor
import no.nav.paw.arbeidssokerregisteret.profilering.personinfo.PersonInfoTjeneste
import no.nav.paw.arbeidssokerregisteret.profilering.utils.ApplicationInfo
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.config.kafka.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.config.kafka.streams.KafkaStreamsFactory
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.Serdes.LongSerde
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.state.Stores
import org.slf4j.LoggerFactory
import java.time.Duration

fun main() {
    val logger = LoggerFactory.getLogger("app")
    logger.info("Starter: {}", ApplicationInfo.id)
    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)
    val streamsBuilder = StreamsBuilder()
        .addStateStore(
            Stores.timestampedKeyValueStoreBuilder(
                Stores.persistentKeyValueStore("periodeTombstoneDelayStore"),
                Serdes.String(),
                Serdes.String()
            )
        )
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfiguration>(APPLICATION_CONFIG_FILE)

    val topology = applicationTopology(
        streamBuilder = streamsBuilder,
        personInfoTjeneste = PersonInfoTjeneste.create(),
        applicationConfiguration = applicationConfig,
        suppressionConfig = SuppressionConfig(
            stateStoreName = "periodeTombstoneDelayStore",
            scheduleInterval = Duration.ofMinutes(1),
            scheduleType = PunctuationType.WALL_CLOCK_TIME,
            suppressFor = Duration.ofHours(1),
            suppressDurationType = SuppressionConfig.Type.ANY
        ) { _, value -> value.avsluttet != null }
    )
    val streamsConfig = KafkaStreamsFactory(applicationConfig.applicationIdSuffix, kafkaConfig)
        .withDefaultKeySerde(LongSerde::class)
        .withDefaultValueSerde(SpecificAvroSerde::class)
    val kafkaStreams = KafkaStreams(topology, streamsConfig.properties)

    kafkaStreams.setUncaughtExceptionHandler { throwable ->
        logger.error("Uventet feil", throwable)
        StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_APPLICATION
    }

    kafkaStreams.start()

    val health = Health(kafkaStreams)

    initKtor(
        kafkaStreamsMetrics = KafkaStreamsMetrics(kafkaStreams),
        prometheusRegistry = prometheusMeterRegistry,
        health = health
    ).start(wait = true)

    logger.info("Applikasjon startet")
}

