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
  val BlockURL = s"http://$publicAddress" + "/block_user"
  val UnblockURL = s"http://$publicAddress" + "/unblock_user"

  var sharedUsername = ""
  var sharedUsername2 = ""
  var sharedUsername3 = ""
  var sharedToken = ""
  var sharedToken2 = ""
  var sharedToken3 = ""

  "GIVEN a user" in {
    sharedUsername = "AAA" + System.currentTimeMillis()
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
    sharedUsername2 = "BBB" + System.currentTimeMillis()
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
    sharedUsername3 = "CCC" + System.currentTimeMillis()
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
    response.status mustBe OK
  }

  "User1 should be able to send a message with user 2" in {
    val data = Json.obj(
      "content" -> "test?"
    )
    val response = await(wsClient.url(URL + "/" + sharedUsername2)
      .withHeaders(("Content-Type","application/json"), ("auth", sharedToken)).post(data))
    response.status mustBe OK
  }

  "User2 should be able to reply to user 1" in {
    val data = Json.obj(
      "content" -> "test!"
    )
    val response = await(wsClient.url(URL + "/" + sharedUsername)
      .withHeaders(("Content-Type","application/json"), ("auth", sharedToken2)).post(data))
    response.status mustBe OK
  }

  "User3 should be able to create a conversation with user 2" in {
    val data = Json.obj(
      "content" -> "test"
    )
    val response = await(wsClient.url(URL + "/" + sharedUsername2)
      .withHeaders(("Content-Type","application/json"), ("auth", sharedToken3)).post(data))
    response.status mustBe OK
  }

  "User1 should not be able to create a conversation with user that does not exits" in {
    val data = Json.obj(
      "content" -> "test"
    )
    val response = await(wsClient.url(URL + "/" + "ahaha")
      .withHeaders(("Content-Type","application/json"), ("auth", sharedToken)).post(data))
    response.status mustBe NOT_FOUND
    assert(response.body == "{\"cause\":\"This user does not exists.\"}")
  }

  //GET USER INBOX

  "User2 should be able to get a view of his messages" in {
    val response = await(wsClient.url(URL)
      .withHeaders(("auth", sharedToken2)).get())
    val tmp = response.body
    response.status mustBe OK
    assert(response.body == "[{\"from\":\"" + sharedUsername + "\"},{\"from\":\"" + sharedUsername3 +"\"}]")
  }

  //GET    /block_user/:username

  "User2 should be able to block User 1" in {
    //first block
    val response = await(wsClient.url(BlockURL + "/" + sharedUsername)
      .withHeaders(("auth", sharedToken2)).get())
    response.status mustBe OK

    //assert the user 1 can no longer send messages to 2
    val data = Json.obj(
      "content" -> "U mad bro?"
    )
    val response2 = await(wsClient.url(URL + "/" + sharedUsername2)
      .withHeaders(("Content-Type","application/json"), ("auth", sharedToken)).post(data))
    response2.status mustBe FORBIDDEN
    assert(response2.body == "{\"cause\":\"This user has blocked you.\"}")
  }

  "User2 should be able to unblock User 1" in {
    //first unblock
    val response = await(wsClient.url(UnblockURL + "/" + sharedUsername)
      .withHeaders(("auth", sharedToken2)).get())
    response.status mustBe OK

    //assert the user 1 can now send messages to 2
    val data = Json.obj(
      "content" -> "U still mad bro?"
    )
    val response2 = await(wsClient.url(URL + "/" + sharedUsername2)
      .withHeaders(("Content-Type","application/json"), ("auth", sharedToken)).post(data))
    response2.status mustBe OK
  }
}
