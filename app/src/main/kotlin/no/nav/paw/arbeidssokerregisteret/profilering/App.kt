package no.nav.paw.arbeidssokerregisteret.profilering

import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.config.kafka.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.config.kafka.KafkaConfig
import org.slf4j.LoggerFactory

fun main() {
    val logger = LoggerFactory.getLogger("main")
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)


}

object ApplicationInfo {
    private val pkg = this::class.java.`package`
    val version: String? = pkg.implementationVersion
    val name: String? = pkg.implementationTitle
}
