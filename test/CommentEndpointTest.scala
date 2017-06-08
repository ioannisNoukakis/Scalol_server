import org.scalatestplus.play._
import play.api.libs.ws.WSClient
import play.api.test.Helpers._
import play.api.libs.json.Json

/**
  * Created by lux on 30/05/2017.
  */
class CommentEndpointTest extends PlaySpec with OneServerPerSuite {
  val wsClient = app.injector.instanceOf[WSClient]
  val publicAddress =  s"localhost:$port"
  val URL = s"http://$publicAddress"+"/comments"

  var sharedUsername = ""
  var sharedToken = ""

  "Given an auth user" in {
    sharedUsername = "user" + System.currentTimeMillis()
    val data = Json.obj(
      "username" -> sharedUsername,
      "mail" -> "user.com",
      "password" -> "mylittlepassword"
    )
    val response = await(wsClient.url(s"http://$publicAddress" + "/user").withHeaders(("Content-Type", "application/json")).post(data))
    response.status mustBe OK
    assert(response.body.startsWith("{\"token\":"))
    sharedToken = response.body.split("[{}\":]")(5)
  }

  //POST
  "Comments endpoint should be able to post comments on a post" in {
    val data = Json.obj(
      "post_id" -> 1,
      "content" -> "REPOST"
    )

    val response = await(wsClient.url(URL)
      .withHeaders(
        ("Content-Type", "application/json"),
        ("auth", sharedToken))
      .post(data))
    response.status mustBe OK
  }

  "Comments endpoint should not be able to post comments on a post if a field is missing" in {
    val data = Json.obj(
      "post_id" -> 1
    )
    val response = await(wsClient.url(URL)
      .withHeaders(
        ("Content-Type", "application/json"),
        ("auth", sharedToken))
      .post(data))
    response.status mustBe BAD_REQUEST
    assert(response.body == "{\"cause\":\"Your body is incomplete or wrong. See our API documentation for a correct version (API v1.0)\"}")
  }

  "Comments endpoint should not be able to post comments on a post if the post does not exists" in {
    val data = Json.obj(
      "post_id" -> -1,
      "content" -> "REPOST2"
    )
    val response = await(wsClient.url(URL)
      .withHeaders(
        ("Content-Type", "application/json"),
        ("auth", sharedToken))
      .post(data))
    response.status mustBe NOT_FOUND
  }

  "Comments endpoint should not be able to post comments on a post if the auth header is missing" in {
    val data = Json.obj(
      "post_id" -> 1,
      "content" -> "REPOST3"
    )
    val response = await(wsClient.url(URL)
      .withHeaders(
        ("Content-Type", "application/json")
      )
      .post(data))
    response.status mustBe BAD_REQUEST
    assert(response.body == "{\"cause\":\"Missing auth.\"}")
  }

  //GET
  "Comments endpoint should be able to retrive comments" in {
    val response = await(wsClient.url(URL + "/1")
      .get())
    response.status mustBe OK
    assert(!response.body.isEmpty)
  }

  "Comments endpoint should not be able to retrive comments from an nonexistant post" in {
    val response = await(wsClient.url(URL + "/-1")
      .get())
    println(response.body)
    response.status mustBe OK
    assert(response.body == "[]")
  }
}