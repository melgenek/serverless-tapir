package sttp.tapir.server.httpapi

import com.amazonaws.services.lambda.runtime.events.{APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse}
import sttp.tapir.server.ServerEndpoint

import scala.util.Try

trait HttpApiFunction {

  val serverEndpoints: List[ServerEndpoint[_, _, _, Nothing, Try]]

  def onEvent(event: APIGatewayV2HTTPEvent): APIGatewayV2HTTPResponse = {
    serverEndpoints.toRoutes(event)
  }

}
