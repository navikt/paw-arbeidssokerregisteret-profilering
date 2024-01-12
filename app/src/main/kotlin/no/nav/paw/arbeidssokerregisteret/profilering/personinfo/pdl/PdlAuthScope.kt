package no.nav.paw.arbeidssokerregisteret.profilering.personinfo.pdl

fun PdlConfig.azureAuthScope(): String =
    "api://${pdlCluster}.${namespace}.${appName}/.default"