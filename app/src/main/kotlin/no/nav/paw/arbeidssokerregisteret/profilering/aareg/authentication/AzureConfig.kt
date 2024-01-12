package no.nav.paw.arbeidssokerregisteret.profilering.aareg.authentication

const val AZURE_CONFIG_FILE = "azure_config.toml"
data class AzureConfig(
    val clientId: String,
    val tokenEndpointUrl: String
)