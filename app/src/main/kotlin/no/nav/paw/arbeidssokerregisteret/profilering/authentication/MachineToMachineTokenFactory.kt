package no.nav.paw.arbeidssokerregisteret.profilering.authentication

fun interface MachineToMachineTokenFactory {
    fun create(scope: String): String
}