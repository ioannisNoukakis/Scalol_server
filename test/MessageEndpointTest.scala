import org.scalatestplus.play._
import play.api.libs.ws.WSClient
import play.api.test.Helpers._
import play.api.libs.json.Json

/**
  * Created by lux on 01/06/2017.
  */
class MessageEndpointTest extends PlaySpec with OneServerPerSuite {
  val wsClient = app.injector.instanceOf[WSClient]
  val publicAddress =  s"localhost:$port"
  val authURL = s"http://$publicAddress"+"/user"
  val URL = s"http://$publicAddress" + "/messages"

  var sharedUsername = ""
  var sharedUsername2 = ""
  var sharedUsername3 = ""
  var sharedToken = ""
  var sharedToken2 = ""
  var sharedToken3 = ""

  "GIVEN a user" in {
    sharedUsername = "user" + System.currentTimeMillis()
    val data = Json.obj(
      "username" -> sharedUsername,
      "mail" -> "user.com",
      "password" -> "mylittlepassword"
    )
    val response = await(wsClient.url(authURL).withHeaders(("Content-Type","application/json")).post(data))
    response.status mustBe OK
    assert(response.body.startsWith("{\"token\":"))
    sharedToken = response.body.split("[{}\":]")(5)
  }

  "GIVEN an other user" in {
    sharedUsername2 = "user" + System.currentTimeMillis()
    val data = Json.obj(
      "username" -> sharedUsername2,
      "mail" -> "user.com",
      "password" -> "mylittlepassword"
    )
    val response = await(wsClient.url(authURL).withHeaders(("Content-Type","application/json")).post(data))
    response.status mustBe OK
    assert(response.body.startsWith("{\"token\":"))
    sharedToken2 = response.body.split("[{}\":]")(5)
  }

  "GIVEN an other other user" in {
    sharedUsername3 = "user" + System.currentTimeMillis()
    val data = Json.obj(
      "username" -> sharedUsername3,
      "mail" -> "user.com",
      "password" -> "mylittlepassword"
    )
    val response = await(wsClient.url(authURL).withHeaders(("Content-Type","application/json")).post(data))
    response.status mustBe OK
    assert(response.body.startsWith("{\"token\":"))
    sharedToken3 = response.body.split("[{}\":]")(5)
  }

  //POST /messages
  "User1 should be able to create a conversation with user 2" in {
    val data = Json.obj(
      "content" -> "test"
    )
    val response = await(wsClient.url(URL + "/" + sharedUsername2)
      .withHeaders(("Content-Type","application/json"), ("auth", sharedToken)).post(data))

    assert(false)
  }
}
