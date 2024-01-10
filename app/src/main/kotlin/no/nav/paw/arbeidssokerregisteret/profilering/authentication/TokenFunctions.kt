package no.nav.paw.arbeidssokerregisteret.profilering.authentication

import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient
import no.nav.paw.arbeidssokerregisteret.profilering.utils.NaisClusterName
import no.nav.paw.arbeidssokerregisteret.profilering.utils.naisClusterName
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

fun m2mTokenFactory(azureConfig: AzureConfig): MachineToMachineTokenFactory {
    val client = azureAdTokenClient(naisClusterName(), azureConfig)
    return MachineToMachineTokenFactory { scope ->
        client.createMachineToMachineToken(scope)
    }
}

fun azureAdTokenClient(naisClusterName: NaisClusterName, azureConfig: AzureConfig): AzureAdMachineToMachineTokenClient {
    return when (naisClusterName) {
        NaisClusterName.PROD_GCP, NaisClusterName.DEV_GCP -> AzureAdTokenClientBuilder.builder()
            .withNaisDefaults()
            .buildMachineToMachineTokenClient()

        else -> AzureAdTokenClientBuilder.builder()
            .withClientId(azureConfig.clientId)
            .withPrivateJwk(createMockRSAKey("azure"))
            .withTokenEndpointUrl(azureConfig.tokenEndpointUrl)
            .buildMachineToMachineTokenClient()
    }
}

fun createMockRSAKey(keyID: String): String? = KeyPairGenerator
    .getInstance("RSA").let {
        it.initialize(2048)
        it.generateKeyPair()
    }.let {
        RSAKey.Builder(it.public as RSAPublicKey)
            .privateKey(it.private as RSAPrivateKey)
            .keyUse(KeyUse.SIGNATURE)
            .keyID(keyID)
            .build()
            .toJSONString()
    }
