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
import java.time.Duration.between
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
        ctx.schedule(Duration.ofMinutes(10), PunctuationType.STREAM_TIME) { tid ->
            val gjeldeneTidspunkt = Instant.ofEpochMilli(tid)
            stateStore.all().use { iterator ->
                iterator.forEach { keyValue ->
                    val compositeKey = keyValue.key
                    val value = keyValue.value
                    if (value.erUtdatert(
                            gjeldeneTidspunkt = gjeldeneTidspunkt,
                            maksAlderForAvsluttetPeriode = Duration.ofHours(1),
                            maksAlderForFrittstaaendeOpplysning = Duration.ofHours(1)
                        )) {
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
        if (newValue.harPeriodeOgOpplysninger()) {
            ctx.forward(record.withValue(newValue))
            //Vi kan få flere opplysninger på samme periode, så vi beholder den.
            store.put(compositeKey, TopicsJoin(newValue.periode, null, null))
        } else {
            store.put(compositeKey, newValue)
        }
    }
}

fun compositeKey(orginalKey: Long, periodeId: UUID) = "$orginalKey:${periodeId}"
infix fun TopicsJoin.mergeTo(existingData: TopicsJoin?): TopicsJoin =
    TopicsJoin(
        periode ?: existingData?.periode,
        profilering ?: existingData?.profilering,
        opplysningerOmArbeidssoeker ?: existingData?.opplysningerOmArbeidssoeker
    )

fun TopicsJoin.erUtdatert(
    gjeldeneTidspunkt: Instant,
    maksAlderForAvsluttetPeriode: Duration,
    maksAlderForFrittstaaendeOpplysning: Duration
): Boolean {
    return if (periode == null && opplysningerOmArbeidssoeker == null) {
        true
    } else {
        val periodeUtlopt = periode?.avsluttet?.tidspunkt
            ?.let { avsluttet -> between(avsluttet, gjeldeneTidspunkt).abs() }
            ?.let { it > maksAlderForAvsluttetPeriode } ?: false
        val frittStaaendeOpplysningForeldet = periode == null &&
                opplysningerOmArbeidssoeker?.sendtInnAv?.tidspunkt
                    ?.let { opplysningsTimestamp -> between(opplysningsTimestamp, gjeldeneTidspunkt).abs() }
                    ?.let { it > maksAlderForFrittstaaendeOpplysning } ?: false
        periodeUtlopt || frittStaaendeOpplysningForeldet
    }
}

fun TopicsJoin.periodeId(): UUID =
    periode?.id
        ?: opplysningerOmArbeidssoeker?.periodeId
        ?: profilering?.periodeId
        ?: throw IllegalStateException("Minst et felt i TopicsJoin må være satt!")

fun TopicsJoin.harPeriodeOgOpplysninger(): Boolean =
    periode != null && opplysningerOmArbeidssoeker != null

class OpplysningerOmArbeidssoekerStateStoreSave(
    stateStoreName: String
) : BaseStateStoreSave(stateStoreName)

class PeriodeStateStoreSave(
    stateStoreName: String
) : BaseStateStoreSave(stateStoreName)