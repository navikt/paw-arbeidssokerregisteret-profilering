package no.nav.paw.arbeidssokerregisteret.profilering.application

import no.nav.paw.arbeidssokerregisteret.api.helpers.v3.TopicsJoin
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Named
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.processor.api.Processor
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.KeyValueStore
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.reflect.KClass

fun KStream<Long, TopicsJoin>.saveAndForwardIfComplete(
    type: KClass<out BaseStateStoreSave>,
    stateStoreName: String,
): KStream<Long, TopicsJoin> {
    val processor = {
        type.java.getDeclaredConstructor(String::class.java)
            .newInstance(stateStoreName)
    }
    return process(processor, Named.`as`(type.simpleName), stateStoreName)
}

sealed class BaseStateStoreSave(
    private val stateStoreName: String
) : Processor<Long, TopicsJoin, Long, TopicsJoin> {
    private var stateStore: KeyValueStore<String, TopicsJoin>? = null
    private var context: ProcessorContext<Long, TopicsJoin>? = null
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun init(context: ProcessorContext<Long, TopicsJoin>?) {
        super.init(context)
        this.context = context
        stateStore = context?.getStateStore(stateStoreName)
        scheduleCleanup(
            requireNotNull(context) { "Context is not initialized" },
            requireNotNull(stateStore) { "State store is not initialized" }
        )
    }

    private fun scheduleCleanup(
        ctx: ProcessorContext<Long, TopicsJoin>,
        stateStore: KeyValueStore<String, TopicsJoin>
    ) {
        ctx.schedule(Duration.ofMinutes(10), PunctuationType.STREAM_TIME) { time ->
            val currentTime = Instant.ofEpochMilli(time)
            stateStore.all().use { iterator ->
                iterator.forEach { keyValue ->
                    val compositeKey = keyValue.key
                    val value = keyValue.value
                    if (value.isOutdated(currentTime = currentTime)) {
                        stateStore.delete(compositeKey)
                    }
                }
            }
        }
    }

    override fun process(record: Record<Long, TopicsJoin>?) {
        val store = requireNotNull(stateStore) { "State store is not initialized" }
        val ctx = requireNotNull(context) { "Context is not initialized" }
        if (record == null) return
        val compositeKey = compositeKey(record.key(), record.value().periodeId())
        val currentValue = store.get(compositeKey)
        val newValue = record.value() mergeTo currentValue
        if (newValue.isComplete()) {
            ctx.forward(record.withValue(newValue))
            // Vi kan få flere opplysninger på samme periode, så vi beholder den.
            store.put(compositeKey, TopicsJoin(newValue.periode, null, null))
        } else {
            store.put(compositeKey, newValue)
        }
    }
}

fun compositeKey(orginalKey: Long, periodeId: UUID) = "$orginalKey:${periodeId}"

class OpplysningerOmArbeidssoekerStateStoreSave(
    stateStoreName: String
) : BaseStateStoreSave(stateStoreName)

class PeriodeStateStoreSave(
    stateStoreName: String
) : BaseStateStoreSave(stateStoreName)