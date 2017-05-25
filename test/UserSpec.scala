import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import play.api.libs.json.Json
import play.api.test.Helpers.{POST, OK, contentAsString, contentType, route, status}
import play.api.test.{FakeRequest, WithApplication}

/**
  * Created by durza9390 on 25.05.2017.
  */
@RunWith(classOf[JUnitRunner])
class UserSpec extends Specification {

  "UserEndpoint" should {

    "allow to create a user" in new WithApplication{
      val userEndpoint = route(FakeRequest(POST, "/")).get

      status(userEndpoint) must equalTo(OK)
      contentType(userEndpoint) must beSome.which(_ == "application/json")
      contentAsString(userEndpoint) must contain (Json.obj("state" -> "Yea i'm up",
        "version" -> "v 1.0").toString())
    }
  }

}
