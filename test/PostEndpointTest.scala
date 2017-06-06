import org.scalatestplus.play._
import play.api.libs.ws.WSClient
import play.api.test.Helpers._
import play.api.libs.json.Json

/**
  * Created by lux on 30/05/2017.
  */
class PostEndpointTest extends PlaySpec with OneServerPerSuite {
  val wsClient = app.injector.instanceOf[WSClient]
  val publicAddress = s"localhost:$port"
  val URL = s"http://$publicAddress" + "/posts"

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

  // POST   /posts
  "Post Endpoint should be able to create a post" in {
    val data = Json.obj(
      "title" -> "Da robbery",
      "image_path" -> "https://img-9gag-fun.9cache.com/photo/aG1m8e0_700b.jpg",
      "nsfw" -> false
    )
    val response = await(wsClient.url(URL)
      .withHeaders(("Content-Type", "application/json"))
      .withHeaders(("auth", sharedToken))
      .post(data))
    response.status mustBe OK
    assert(response.body.startsWith("{\"location:\":\"nixme.ddns.net/posts/"))
    assert(response.body.endsWith("\"owner: \":\"" + sharedUsername + "\"}"))
  }

  "Post Endpoint should not be able to create a post if json is incomplete" in {
    val data = Json.obj(
      "image_path" -> "https://img-9gag-fun.9cache.com/photo/aG1m8e0_700b.jpg",
      "nsfw" -> false
    )
    val response = await(wsClient.url(URL)
      .withHeaders(("Content-Type", "application/json"))
      .withHeaders(("auth", sharedToken))
      .post(data))
    response.status mustBe BAD_REQUEST
    assert(response.body == "{\"cause\":\"Your body is incomplete or wrong. See our API documentation for a correct version (API v1.0)\"}")
  }

  "Post Endpoint should not be able to create a post if auth is missing" in {
    val data = Json.obj(
      "title" -> "Da robbery",
      "image_path" -> "https://img-9gag-fun.9cache.com/photo/aG1m8e0_700b.jpg",
      "nsfw" -> false
    )
    val response = await(wsClient.url(URL)
      .withHeaders(("Content-Type", "application/json"))
      .post(data))
    response.status mustBe BAD_REQUEST
    assert(response.body == "{\"cause\":\"Missing auth.\"}")
  }

  //GET    /posts
  "Post Endpoint should be able to get latest posts" in {
    val response = await(wsClient.url(URL)
      .get())
    response.status mustBe OK
    assert(!response.body.isEmpty)
  }

  //GET    /posts/:post_id
  "Post Endpoint should be able to get a specific post" in {
    val response = await(wsClient.url(URL+"/1")
      .get())
    response.status mustBe OK
    assert(!response.body.isEmpty)
  }

  "Post Endpoint should not be able to get a specific post if it does not exists" in {
    val response = await(wsClient.url(URL+"/-1")
      .get())
    response.status mustBe NOT_FOUND
  }

  //get    /upvote/:post_id
  "Post Endpoint should be able to upvote a specific post" in {
    val response = await(wsClient.url(s"http://$publicAddress" + "/upvote/1")
      .withHeaders(("auth", sharedToken))
      .get())
    response.status mustBe OK
  }

  "Post Endpoint should not be able to upvote a specific post twice" in {
    val response = await(wsClient.url(s"http://$publicAddress" + "/upvote/1")
      .withHeaders(("auth", sharedToken))
      .get())
    response.status mustBe FORBIDDEN
  }

  "Post Endpoint should not be able to upvote a specific post if it does not exists" in {
    val response = await(wsClient.url(s"http://$publicAddress" + "/upvote/-1")
      .withHeaders(("auth", sharedToken))
      .get())
    response.status mustBe NOT_FOUND
    assert(response.body == "{\"cause\":\"Nonexistent post.\"}")
  }

  //PUT    /downvote/:post_id
  "Post Endpoint should be able to downvote a specific post" in {
    val response = await(wsClient.url(s"http://$publicAddress" + "/downvote/1")
      .withHeaders(("auth", sharedToken))
      .get())
    response.status mustBe OK
  }

  "Post Endpoint should not be able to downvote a specific post if it does not exists" in {
    val response = await(wsClient.url(s"http://$publicAddress" + "/downvote/-1")
      .withHeaders(("auth", sharedToken))
      .get())
    response.status mustBe NOT_FOUND
    assert(response.body == "{\"cause\":\"Nonexistent post.\"}")
  }

  //POST   /upload
  //FIXME do this one too pls.
  "Post Endpoint should not be able to upload a picture" in {
    assert(false)
  }
}