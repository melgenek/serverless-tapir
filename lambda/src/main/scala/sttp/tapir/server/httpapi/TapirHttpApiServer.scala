package sttp.tapir.server.httpapi

import com.amazonaws.services.lambda.runtime.events.{APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse}
import sttp.model.StatusCode
import sttp.tapir.monad.MonadError
import sttp.tapir.server.internal.{DecodeInputs, DecodeInputsResult, InputValues, InputValuesResult}
import sttp.tapir.server.{DecodeFailureContext, DecodeFailureHandling, ServerDefaults, ServerEndpoint}
import sttp.tapir.{DecodeResult, EndpointIO, EndpointInput}

import scala.util.{Failure, Success, Try}

object TryMonadError extends MonadError[Try] {
  override def unit[T](t: T): Try[T] = Success(t)

  override def map[T, T2](fa: Try[T])(f: T => T2): Try[T2] = fa.map(f)

  override def flatMap[T, T2](fa: Try[T])(f: T => Try[T2]): Try[T2] = fa.flatMap(f)

  override def error[T](t: Throwable): Try[T] = Failure(t)

  override protected def handleWrappedError[T](rt: Try[T])(h: PartialFunction[Throwable, Try[T]]): Try[T] = rt.recoverWith(h)
}

trait TapirHttpApiServer {

  implicit class RichHttpApiServerEndpoint[I, E, O](endpoint: ServerEndpoint[I, E, O, Nothing, Try]) {
    def toRoute: Route = {
      new Route {
        override def isDefinedAt(event: APIGatewayV2HTTPEvent): Boolean = {
          DecodeInputs(endpoint.input, new HttpApiDecodeInputsContext(event)) match {
            case _: DecodeInputsResult.Values => true
            case _: DecodeInputsResult.Failure => false
          }
        }

        override def apply(event: APIGatewayV2HTTPEvent): APIGatewayV2HTTPResponse = {
          decodeBody(event, DecodeInputs(endpoint.input, new HttpApiDecodeInputsContext(event))) match {
            case values: DecodeInputsResult.Values =>
              InputValues(endpoint.input, values) match {
                case InputValuesResult.Value(params, _) => valueToResponse(params.asAny)
                case InputValuesResult.Failure(input, failure) => handleDecodeFailure(input, failure)
              }
            case DecodeInputsResult.Failure(input, failure) => handleDecodeFailure(input, failure)
          }
        }
      }
    }

    private def decodeBody(event: APIGatewayV2HTTPEvent, result: DecodeInputsResult): DecodeInputsResult = {
      result match {
        case values: DecodeInputsResult.Values =>
          values.bodyInput match {
            case Some(bodyInput@EndpointIO.Body(bodyType, codec, _)) =>
              codec.decode(HttpApiRequestToRawBody(event, bodyType)) match {
                case DecodeResult.Value(bodyV) => values.setBodyInputValue(bodyV)
                case failure: DecodeResult.Failure => DecodeInputsResult.Failure(bodyInput, failure)
              }
            case None => values
          }
        case failure: DecodeInputsResult.Failure => failure
      }
    }

    def handleDecodeFailure(input: EndpointInput[_], failure: DecodeResult.Failure): APIGatewayV2HTTPResponse = {
      val decodeFailureCtx = DecodeFailureContext(input, failure)
      println(decodeFailureCtx)
      ServerDefaults.decodeFailureHandler(decodeFailureCtx) match {
        case DecodeFailureHandling.NoMatch =>
          OutputToHttpApiResponse(ServerDefaults.StatusCodes.error)
        case DecodeFailureHandling.RespondWithResponse(output, value) =>
          OutputToHttpApiResponse(ServerDefaults.StatusCodes.error, output, value)
      }
    }

    private def valueToResponse(value: Any): APIGatewayV2HTTPResponse = {
      endpoint.logic(TryMonadError)(value.asInstanceOf[I]) match {
        case Success(Right(result)) => OutputToHttpApiResponse(ServerDefaults.StatusCodes.success, endpoint.output, result)
        case Success(Left(err)) => OutputToHttpApiResponse(ServerDefaults.StatusCodes.error, endpoint.errorOutput, err)
        case Failure(e) => OutputToHttpApiResponse(StatusCode.InternalServerError, e.getMessage)
      }
    }
  }

  implicit class RichHttpApiServerEndpoints[I, E, O](serverEndpoints: List[ServerEndpoint[_, _, _, Nothing, Try]]) {
    def toRoutes: Route = {
      serverEndpoints
        .map(_.toRoute)
        .foldRight(EmptyRoute)(_ orElse _)
    }
  }

}
