package sttp.tapir.server

import com.amazonaws.services.lambda.runtime.events.{APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse}
import sttp.model.StatusCode

import scala.jdk.CollectionConverters._

package object httpapi extends TapirHttpApiServer {

  type Route = PartialFunction[APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse]

  val EmptyRoute: Route = {
    case _ => OutputToHttpApiResponse(StatusCode.NotFound)
  }

  implicit class MapOps(val map: java.util.Map[String, String]) extends AnyVal {
    def getIgnoreCase(name: String): Option[String] = {
      map.asScala.collectFirst { case (key, value) if key.equalsIgnoreCase(name) => value }
    }
  }

}
