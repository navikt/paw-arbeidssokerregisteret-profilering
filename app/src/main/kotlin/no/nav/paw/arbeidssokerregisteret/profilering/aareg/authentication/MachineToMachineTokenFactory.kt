package no.nav.paw.arbeidssokerregisteret.profilering.aareg.authentication

fun interface MachineToMachineTokenFactory {
    fun create(scope: String): String
}