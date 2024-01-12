package no.nav.paw.arbeidssokerregisteret.profilering.utils

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroSerializer
import no.nav.paw.config.kafka.KafkaFactory
import org.apache.kafka.common.serialization.LongDeserializer
import org.apache.kafka.common.serialization.LongSerializer

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
