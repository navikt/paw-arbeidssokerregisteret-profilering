package no.nav.paw.arbeidssokerregisteret.profilering.utils

import io.micrometer.core.instrument.binder.MeterBinder

data class AdditionalMeterBinders(val binders: List<MeterBinder> = emptyList()) {

    constructor(vararg binders: MeterBinder) : this(binders.toList())
    operator fun plus(binder: MeterBinder) = copy(binders = binders + binder)
}
