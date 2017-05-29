import org.scalatestplus.play._
import play.api.libs.ws.WSClient
import play.api.test.Helpers._


/**
  * Created by durza9390 on 29.05.2017.
  */
class ApplicationEndpointTest extends PlaySpec with OneServerPerSuite {

  val wsClient = app.injector.instanceOf[WSClient]
  val publicAddress =  s"localhost:$port"
  val URL = s"http://$publicAddress"

  "Application Endpoint should alway be ok" in {
    // await is from play.api.test.FutureAwaits
    val response = await(wsClient.url(URL).get())

    response.status mustBe OK
  }
}
