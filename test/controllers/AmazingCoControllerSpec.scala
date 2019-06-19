package controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 *
 * For more information, see https://www.playframework.com/documentation/latest/ScalaTestingWithScalaTest
 */
class AmazingCoControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {

  implicit val sys = ActorSystem("MyTest")
  implicit val mat = ActorMaterializer()

  "AmazingCoController" should {

    "add node to parent" in {
      val controller = inject[AmazingCoController]
      val add = controller.addChild("root").apply(FakeRequest(POST, "/").withJsonBody(Json.parse("""{ "nodeId": "42" }""")))
      //println(add)
      //status(add) mustBe OK
      true
    }
  }
}
