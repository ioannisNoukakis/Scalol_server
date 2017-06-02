import org.scalatestplus.play._
import play.api.libs.ws.WSClient
import play.api.test.Helpers._
import play.api.libs.json.Json

/**
  * Created by durza9390 on 29.05.2017.
  */
class UserEndpointTest extends PlaySpec with OneServerPerSuite {

  val wsClient = app.injector.instanceOf[WSClient]
  val publicAddress =  s"localhost:$port"
  val URL = s"http://$publicAddress"+"/user"
  val authURL = s"http://$publicAddress" + "/auth"

  var sharedUsername = ""
  var sharedUsername2 = ""
  var sharedToken = ""
  var sharedToken2 = ""

  // -> POST
  "User Endpoint should be able to create a user" in {
    sharedUsername = "user" + System.currentTimeMillis()
    val data = Json.obj(
      "username" -> sharedUsername,
      "mail" -> "user.com",
      "password" -> "mylittlepassword"
    )
    val response = await(wsClient.url(URL).withHeaders(("Content-Type","application/json")).post(data))
    response.status mustBe OK
    assert(response.body.startsWith("{\"token\":"))
    sharedToken = response.body.split("[{}\":]")(5)
  }

  "User Endpoint should be able to create a second user for the tests" in {
    sharedUsername2 = "user" + System.currentTimeMillis()
    val data = Json.obj(
      "username" -> sharedUsername2,
      "mail" -> "user.com",
      "password" -> "mylittlepassword"
    )
    val response = await(wsClient.url(URL).withHeaders(("Content-Type","application/json")).post(data))
    response.status mustBe OK
    assert(response.body.startsWith("{\"token\":"))
    sharedToken2 = response.body.split("[{}\":]")(5)
  }

  "User Endpoint should not be able to create a user if username is missing" in {

    val data = Json.obj(
      "mail" -> "user.com",
      "password" -> "mylittlepassword"
    )
    val response = await(wsClient.url(URL).withHeaders(("Content-Type","application/json")).post(data))
    response.status mustBe BAD_REQUEST
    assert(response.body == "{\"cause\":\"Your body is incomplete or wrong. See our API documentation for a correct version (API v1.0)\"}")
  }

  "User Endpoint should not be able to create a user if the username is already used" in {

    val data = Json.obj(
      "username" -> sharedUsername,
      "mail" -> "user.com",
      "password" -> "mylittlepassword"
    )
    val response = await(wsClient.url(URL).withHeaders(("Content-Type","application/json")).post(data))
    response.status mustBe CONFLICT
    assert(response.body == "{\"cause\":\"Username already taken.\"}")
  }

  // -> /GET/username
  "User Endpoint should be able to retrieve a given user" in {
    val response = await(wsClient.url(URL + "/" + sharedUsername).get())
    response.status mustBe OK
    assert(response.body.startsWith("{\"user\":{\"username\":\"" + sharedUsername +"\",\"mail\":\"user.com\",\"password\":null"))
  }

  "User Endpoint should not be able to retrieve a nonexistent user" in {
    val response = await(wsClient.url(URL + "/" + System.currentTimeMillis()).get())
    response.status mustBe NOT_FOUND
    assert(response.body == "{\"cause\":\"The following user does not exists.\"}")
  }

  // -> /PATCH

  "User Endpoint should be able to patch a user" in {
    sharedUsername = "Palapin"+System.currentTimeMillis()
    val data = Json.obj(
      "username" -> sharedUsername,
      "mail" -> "Palapin.com",
      "password" -> "lapin"
    )
    val response = await(wsClient.url(URL).withHeaders(("Content-Type","application/json"),
      ("auth",sharedToken))
      .patch(data))
    response.status mustBe OK
  }

  "User Endpoint should not be able to patch a user with the same username" in {
    val data = Json.obj(
      "username" -> sharedUsername,
      "mail" -> "Palapin.com",
      "password" -> "lapin"
    )
    val response = await(wsClient.url(URL).withHeaders(("Content-Type","application/json"),
      ("auth",sharedToken2))
      .patch(data))
    response.status mustBe CONFLICT
    assert(response.body == "{\"cause\":\"Username already taken.\"}")
  }

  // -> /POST/auth
  "User Endpoint should be able to auth a user" in {

    val data = Json.obj(
      "username" -> sharedUsername,
      "mail" -> "user.com",
      "password" -> "lapin"
    )
    val response = await(wsClient.url(authURL).withHeaders(("Content-Type","application/json")).post(data))
    response.status mustBe OK
    assert(response.body.startsWith("{\"token\":"))
    sharedToken = response.body.split("[{}\":]")(5)
  }

  "User Endpoint should not be able to auth a user with a wrong password" in {

    val data = Json.obj(
      "username" -> sharedUsername,
      "mail" -> "user.com",
      "password" -> "wfv"
    )
    val response = await(wsClient.url(authURL).withHeaders(("Content-Type","application/json")).post(data))
    response.status mustBe FORBIDDEN
    assert(response.body == "{\"cause\":\"Invalid password or username\"}")
  }

  "User Endpoint should be able to auth if a filed is missing" in {

    val data = Json.obj(
      "mail" -> "user.com",
      "password" -> "mylittlepassword"
    )
    val response = await(wsClient.url(authURL).withHeaders(("Content-Type","application/json")).post(data))
    response.status mustBe BAD_REQUEST
    assert(response.body == "{\"cause\":\"Your body is incomplete or wrong. See our API documentation for a correct version (API v1.0)\"}")
  }

  // -> DELETE
  "User Endpoint should not be able to delete a user with an invalid token header" in {
    val response = await(wsClient.url(URL).withHeaders(("Content-Type","application/json"), ("auth","wqgweg")).delete())
    response.status mustBe FORBIDDEN
    assert(response.body == "{\"cause\":\"Invalid auth.\"}")
  }

  "User Endpoint should be able to delete a user" in {
    val response = await(wsClient.url(URL).withHeaders(("Content-Type","application/json"), ("auth",sharedToken)).delete())
    response.status mustBe OK
  }
}