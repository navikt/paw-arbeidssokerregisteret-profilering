package no.nav.paw.arbeidssokerregisteret.profilering.utils

object ApplicationInfo {
    private val pkg = this::class.java.`package`
    val version: String? = pkg.implementationVersion
    val name: String? = pkg.implementationTitle
    val id = "$name-$version"
}