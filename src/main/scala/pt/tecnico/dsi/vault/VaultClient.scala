package pt.tecnico.dsi.vault

import cats.effect.Sync
import org.http4s._
import org.http4s.client.Client

//TODO: Some operations don't require a token. So the fact that we are requiring one might be misleading
//maybe we should extract them to the companion object
class VaultClient[F[_]: Sync](val baseUri: Uri, val token: String)(implicit client: Client[F]) { self =>
  implicit val tokenHeader: Header = Header("X-Vault-Token", token)

  val uri = baseUri / "v1"

  object sys {
    val uri: Uri = self.uri / "sys"

    import pt.tecnico.dsi.vault.sys._

    val init = new Init[F](uri / "init") // Does not require a token
    val health = new Health[F](uri / "health") // Does not require a token
    val leader = new Leader[F](uri) // One endpoint requires a token, the other does not
    val seal = new Seal[F](uri) // Does not require a token
    val generateRoot = new GenerateRoot[F](uri / "generate-root") // Does not require a token
    val leases = new Leases[F](uri / "leases")
    val policy = new Policy[F](uri / "policy")
    val auth = new Auth[F](uri / "auth")
    val mounts = new Mounts[F](uri / "mounts")
    val keys = new Keys[F](uri) // One endpoint requires a token, the other does not
    val rekey = new Rekey[F](uri / "rekey") // Does not require a token
  }

  object authMethods {
    val uri: Uri = self.uri / "auth"

    import pt.tecnico.dsi.vault.authMethods.approle.AppRole
    import pt.tecnico.dsi.vault.authMethods.token.Token

    // The Token auth method is always mounted at this location. And cannot be changed.
    val token: Token[F] = new Token[F](uri / "token")
    def appRole(at: String): AppRole[F] = new AppRole[F](uri.withPath(uri.path + "/" + at))
  }

  object secretEngines {
    import pt.tecnico.dsi.vault.secretEngines.consul.Consul
    import pt.tecnico.dsi.vault.secretEngines.kv.KeyValueV1
    import pt.tecnico.dsi.vault.secretEngines.pki.PKI
    import pt.tecnico.dsi.vault.secretEngines.databases._

    def kv(at: String): KeyValueV1[F] = new KeyValueV1[F](uri.withPath(uri.path + "/" + at))
    def consul(at: String): Consul[F] = new Consul[F](uri.withPath(uri.path + "/" + at))
    def pki(at: String): PKI[F] = new PKI[F](uri.withPath(uri.path + "/" + at))
    def mysql(at: String): MySql[F] = new MySql[F](uri.withPath(uri.path + "/" + at))
  }
}