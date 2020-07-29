package sttp.tapir.server.httpapi

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import sttp.model.{Method, QueryParams}
import sttp.tapir.model.ServerRequest
import sttp.tapir.server.internal.DecodeInputsContext

import scala.jdk.CollectionConverters._

private[httpapi] class HttpApiDecodeInputsContext(event: APIGatewayV2HTTPEvent, pathConsumed: Int = 0) extends DecodeInputsContext {
  override def method: Method = Method(event.getRequestContext.getHttp.getMethod.toUpperCase)

  override def nextPathSegment: (Option[String], DecodeInputsContext) = {
    val path = event.getRawPath.drop(pathConsumed)
    val nextStart = path.dropWhile(_ == '/')
    val segment = nextStart.split("/", 2) match {
      case Array("") => None
      case Array(s) => Some(s)
      case Array(s, _) => Some(s)
    }
    val charactersConsumed = segment.map(_.length).getOrElse(0) + (path.length - nextStart.length)

    (segment, new HttpApiDecodeInputsContext(event, pathConsumed + charactersConsumed))
  }

  override def header(name: String): List[String] = {
    event.getHeaders.getIgnoreCase(name).toList.flatMap(_.split(",").toList)
  }

  override def headers: Seq[(String, String)] = event.getHeaders.asScala.toList

  override def queryParameter(name: String): Seq[String] = {
    event.getQueryStringParameters.getIgnoreCase(name).toList.flatMap(_.split(",").toList)
  }

  override def queryParameters: QueryParams = QueryParams.fromMap(event.getQueryStringParameters.asScala.toMap)

  override def bodyStream: Any = {
    throw new UnsupportedOperationException("Trying to read streaming body from a non-streaming request")
  }

  override def serverRequest: ServerRequest = new HttpApiServerRequest(event)
}
