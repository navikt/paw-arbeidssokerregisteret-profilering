package no.nav.paw.arbeidssokerregisteret.profilering

fun main() {
    println("${ApplicationInfo.name}, versjon: ${ApplicationInfo.version}")
}
object ApplicationInfo {
    private val pkg = this::class.java.`package`
    val version: String? = pkg.implementationVersion
    val name: String? = pkg.implementationTitle
}
