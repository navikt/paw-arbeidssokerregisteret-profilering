package no.nav.paw.arbeidssokerregisteret.profilering.utils

enum class NaisClusterName {
    UNDEFINED,
    PROD_GCP,
    DEV_GCP,
}

fun naisClusterName(): NaisClusterName =
    when (System.getenv("NAIS_CLUSTER_NAME")) {
        "prod-gcp" -> NaisClusterName.PROD_GCP
        "dev-gcp" -> NaisClusterName.DEV_GCP
        else -> NaisClusterName.UNDEFINED
    }