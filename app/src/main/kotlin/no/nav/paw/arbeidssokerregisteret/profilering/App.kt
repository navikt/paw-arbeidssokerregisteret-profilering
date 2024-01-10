package no.nav.paw.arbeidssokerregisteret.profilering

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroSerializer
import no.nav.paw.aareg.AaregClient
import no.nav.paw.arbeidssokerregisteret.profilering.aareg.AAREG_CONFIG_FILE
import no.nav.paw.arbeidssokerregisteret.profilering.aareg.AaRegClientConfig
import no.nav.paw.arbeidssokerregisteret.profilering.authentication.AZURE_CONFIG_FILE
import no.nav.paw.arbeidssokerregisteret.profilering.authentication.m2mTokenFactory
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.config.kafka.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.config.kafka.KafkaFactory
import org.apache.kafka.common.serialization.LongDeserializer
import org.apache.kafka.common.serialization.LongSerializer
import org.slf4j.LoggerFactory

fun main() {
    val logger = LoggerFactory.getLogger("main")
    val (producer, consumer) = with(KafkaFactory(loadNaisOrLocalConfiguration(KAFKA_CONFIG_WITH_SCHEME_REG))) {
        profilieringResultatProducer() to opplysningerMottattConsumer()
    }
    val m2mTokenFactory = m2mTokenFactory(loadNaisOrLocalConfiguration(AZURE_CONFIG_FILE))
    val aaRegClient = with (loadNaisOrLocalConfiguration<AaRegClientConfig>(AAREG_CONFIG_FILE)) {
        AaregClient(url) { m2mTokenFactory.create(scope) }
    }
}

fun KafkaFactory.opplysningerMottattConsumer() = createConsumer(
    groupId = "paw-arbeidssokerregisteret-profilering",
    clientId = ApplicationInfo.id,
    keyDeserializer = LongDeserializer::class,
    valueDeserializer = KafkaAvroDeserializer::class
)

fun KafkaFactory.profilieringResultatProducer() = createProducer(
    clientId = ApplicationInfo.id,
    keySerializer = LongSerializer::class,
    valueSerializer = KafkaAvroSerializer::class
)

object ApplicationInfo {
    private val pkg = this::class.java.`package`
    val version: String? = pkg.implementationVersion
    val name: String? = pkg.implementationTitle
    val id = "$name-$version"
}
