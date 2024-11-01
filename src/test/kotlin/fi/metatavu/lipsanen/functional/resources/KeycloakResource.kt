package fi.metatavu.lipsanen.functional.resources

import dasniko.testcontainers.keycloak.KeycloakContainer
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.Wait

internal class SpecifiedSMTPContainer(image: String) : GenericContainer<SpecifiedSMTPContainer>(image)

/**
 * Quarkus test resource for providing Keycloak services connected to smtp server to receive emails
 */
class KeycloakResource : QuarkusTestResourceLifecycleManager {
    private val smtp = SpecifiedSMTPContainer("mailhog/mailhog")

    override fun start(): Map<String, String> {
        val network = Network.newNetwork()

        smtp.waitingFor(Wait.forHttp("/").forPort(8025))
            .withCreateContainerCmdModifier { cmd -> cmd.withHostName("mail-server") }.withExposedPorts(1025, 8025)
            .withNetwork(network).start()

        keycloak.withNetwork(network).start()
        val config: MutableMap<String, String> = HashMap()
        config["quarkus.oidc.auth-server-url"] = "${keycloak.authServerUrl}/realms/lipsanen-user-management"
        config["quarkus.oidc.client-id"] = "api"

        config["lipsanen.keycloak.admin.realm"] = "lipsanen-user-management"
        config["lipsanen.keycloak.admin.secret"] = "mTYSmcZv0k0dce42gS7GGEje2t35a6yP"
        config["lipsanen.keycloak.admin.client"] = "api"
        config["lipsanen.keycloak.admin.user"] = "admin-api"
        config["lipsanen.keycloak.admin.password"] = "test"

        config["lipsanen.keycloak.defaultRole"] = "default-roles-lipsanen-user-management"
        config["lipsanen.smtp.http.test-host"] = smtp.host
        config["lipsanen.smtp.http.test-port"] = smtp.getMappedPort(8025).toString()
        return config
    }

    override fun stop() {
        keycloak.stop()
    }

    /*
    Note: in this keycloak version main page shows Local access required error
    but the page at http://localhost:{port}/admin/master/console/ works fine
     */
    companion object {
        var keycloak: KeycloakContainer = KeycloakContainer()
            .withRealmImportFile("kc.json")
    }
}