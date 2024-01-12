package no.nav.paw.arbeidssokerregisteret.profilering

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.kafka.KafkaStreamsMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.runBlocking
import no.nav.paw.aareg.AaregClient
import no.nav.paw.arbeidssokerregisteret.profilering.personinfo.aareg.AaRegClientConfig
import no.nav.paw.arbeidssokerregisteret.profilering.application.APPLICATION_CONFIG_FILE
import no.nav.paw.arbeidssokerregisteret.profilering.application.ApplicationConfiguration
import no.nav.paw.arbeidssokerregisteret.profilering.application.applicationTopology
import no.nav.paw.arbeidssokerregisteret.profilering.personinfo.authentication.AZURE_CONFIG_FILE
import no.nav.paw.arbeidssokerregisteret.profilering.personinfo.authentication.m2mTokenFactory
import no.nav.paw.arbeidssokerregisteret.profilering.application.SuppressionConfig
import no.nav.paw.arbeidssokerregisteret.profilering.personinfo.PersonInfoTjeneste
import no.nav.paw.arbeidssokerregisteret.profilering.utils.AdditionalMeterBinders
import no.nav.paw.arbeidssokerregisteret.profilering.utils.ApplicationInfo
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.config.kafka.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.config.kafka.streams.KafkaStreamsFactory
import org.apache.kafka.common.serialization.Serdes.LongSerde
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.processor.PunctuationType
import org.slf4j.LoggerFactory
import java.time.Duration

fun main() {
    val logger = LoggerFactory.getLogger("app")
    logger.info("Starter: {}", ApplicationInfo.id)
    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)
    val streamsBuilder = StreamsBuilder()
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
    //TODO: Av en eller annen grunn liker ikke kompilatoren:
    // KafkaStreamsFactory(applicationConfig.applicationIdSuffix, kafkaConfig)
    val streamsConfig = (::KafkaStreamsFactory)(applicationConfig.applicationIdSuffix, kafkaConfig)
        .withDefaultKeySerde(SpecificAvroSerde::class)
        .withDefaultKeySerde(LongSerde::class)
    val streams = KafkaStreams(topology, streamsConfig.properties)
    val additionalMeterBinders = AdditionalMeterBinders(
        KafkaStreamsMetrics(streams),
        JvmMemoryMetrics(),
        JvmGcMetrics(),
        ProcessorMetrics()
    )
    logger.info("konfigurasjon lastet og avhenigheter opprettet")
}

