package no.nav.paw.arbeidssokerregisteret.profilering.aareg

const val AAREG_CONFIG_FILE = "aareg_client_config.toml"
data class AaRegClientConfig(
    val url: String,
    val scope: String
)