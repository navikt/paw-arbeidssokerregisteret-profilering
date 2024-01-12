package no.nav.paw.arbeidssokerregisteret.profilering.utils

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

fun <K, V> Consumer<K, V>.asSequence(
    stop: AtomicBoolean,
    timeout: Duration = Duration.ofSeconds(1)
): Sequence<ConsumerRecord<K, V>> {
    return generateSequence {
        commitSync()
        if (stop.get()) {
            null
        } else {
            poll(timeout)
        }
    }.flatten()
}
