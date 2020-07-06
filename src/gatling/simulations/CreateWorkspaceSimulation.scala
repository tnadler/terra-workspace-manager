import com.google.auth.oauth2.GoogleCredentials
import java.io.ByteArrayInputStream
import java.util.UUID
import com.typesafe.config.{Config, ConfigFactory}
import io.gatling.http.Predef._
import io.gatling.core.Predef._
import scala.collection.JavaConverters._
import sys.process._

class CreateWorkspaceSimulation extends Simulation {
  val config: Config = ConfigFactory.load("application.conf")
  val wsmBaseUrl = config.getString("dev.terra.wsmBaseUrl")
  val email = config.getString("dev.sam.email")
  //val serviceAccountJson = System.getenv(
  //  config.getString("dev.sam.firecloudServiceAccount"))
  val serviceAccountFilePath = System.getenv(
    config.getString("dev.sam.serviceAccountFilePath"))
  println(serviceAccountFilePath)
  val bufferedSource = scala.io.Source.fromFile(serviceAccountFilePath)
  val serviceAccountJson = bufferedSource.getLines.mkString
  bufferedSource.close
  //val concurrency = System.getenv("GATLING_CONCURRENCY").toInt
  val concurrency = 10

  //println(wsmBaseUrl)
  //println(email)
  //println(serviceAccountJson)

  val scopes = List(
    "profile",
    "email",
    "openid"
    //"https://www.googleapis.com/auth/devstorage.full_control",
    //"https://www.googleapis.com/auth/cloud-platform"
  )
  val credentials =
    GoogleCredentials.fromStream(
      new ByteArrayInputStream(serviceAccountJson.getBytes()))
      .createScoped(scopes.asJava)
      .createDelegated(email)

  credentials.refreshIfExpired()
  val newAccessToken = credentials.getAccessToken
  val authToken = newAccessToken.getTokenValue

  //println(authToken)

  val workspaceIdFeeder = Iterator.continually(
    Map("workspaceId" -> UUID.randomUUID)
  )
  val tokenFeeder = Iterator.continually(
    Map("authToken" -> authToken)
  )

  val api : String = "api/workspaces/v1"
  val body = """
               |{
               |  "id": "${workspaceId}",
               |  "authToken": "${authToken}",
               |  "spendProfile": "${workspaceId}",
               |  "policies": [ "${workspaceId}" ]
               |}
               |""".stripMargin

  val httpConf = http.baseUrl(wsmBaseUrl)
  val scn = scenario("Create Workspace Simulation")
    .feed(workspaceIdFeeder)
    .feed(tokenFeeder)
    .exec(http("post_workspaces")
      .post(api)
      .header("Authorization", "Bearer ${authToken}")
      .body(StringBody(body)).asJson
      .check(status.is(200)))
  setUp(
    scn.inject(atOnceUsers(concurrency))
  ).protocols(httpConf)
}
