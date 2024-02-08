package no.nav.paw.arbeidssokerregisteret.profilering.application

import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.processor.api.Processor
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.TimestampedKeyValueStore
import org.apache.kafka.streams.state.ValueAndTimestamp
import java.time.Duration

fun <K, V> KStream<K, V>.conditionallySuppress(
    suppressionConfig: SuppressionConfig<K, V>
): KStream<K, V> {
    val processorSupplier = { ConditionallySuppress(suppressionConfig) }
    return process(processorSupplier, suppressionConfig.stateStoreName)
}
class ConditionallySuppress<K, V>(
    private val config: SuppressionConfig<K, V>,
) : Processor<K, V, K, V> {
    private var stateStore: TimestampedKeyValueStore<K, V>? = null
    private var context: ProcessorContext<K, V>? = null

    override fun init(context: ProcessorContext<K, V>?) {
        super.init(context)
        this.context = context
        stateStore = context?.getStateStore(config.stateStoreName)
        scheduleEmitSuppressedRecords(
            requireNotNull(context) { "Context is not initialized" },
            requireNotNull(stateStore) { "State store is not initialized" }
        )
    }

    private fun scheduleEmitSuppressedRecords(
        ctx: ProcessorContext<K, V>,
        stateStore: TimestampedKeyValueStore<K, V>
    ) {
        ctx.schedule(config.scheduleInterval, config.scheduleType) {
            stateStore.all().use { iterator ->
                iterator.asSequence()
                    .filter { kv ->
                        val suppressUntil = kv.value.timestamp() + config.suppressFor.toMillis()
                        fun wallTimePassed(): Boolean = suppressUntil < ctx.currentSystemTimeMs()
                        fun streamTimePassed(): Boolean = suppressUntil < ctx.currentStreamTimeMs()
                        when (config.suppressDurationType) {
                            SuppressionConfig.Type.WALL_CLOCK_TIME -> wallTimePassed()
                            SuppressionConfig.Type.STREAM_TIME -> streamTimePassed()
                            SuppressionConfig.Type.ANY -> wallTimePassed() || streamTimePassed()
                        }
                    }.onEach { kv ->
                        ctx.forward(Record(kv.key, kv.value.value(), kv.value.timestamp()))
                    }.forEach { kv ->
                        stateStore.delete(kv.key)
                    }
            }
        }
    }

    override fun process(record: Record<K, V>?) {
        val store = requireNotNull(stateStore) { "State store is not initialized" }
        val ctx = requireNotNull(context) { "Context is not initialized" }
        if (record == null) {
            return
        } else {
            if (config.condition(record.key(), record.value())) {
                store.put(record.key(), ValueAndTimestamp.make(record.value(), record.timestamp()))
            } else {
                store.get(record.key())?.let { valueAndTimestamp ->
                    record.withValue(valueAndTimestamp.value())
                        .withTimestamp(record.timestamp() - 1L)
                }?.run(ctx::forward)
                ctx.forward(record)
            }
        }
    }
}

class SuppressionConfig<K, V>(
    val stateStoreName: String,
    val scheduleInterval: Duration,
    val scheduleType: PunctuationType,
    val suppressFor: Duration,
    val suppressDurationType: Type,
    val condition: (K, V) -> Boolean
) {
    enum class Type {
        WALL_CLOCK_TIME,
        STREAM_TIME,
        ANY
    }
}