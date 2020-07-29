package sttp.tapir.server.httpapi

import java.net.{InetSocketAddress, URI}

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import sttp.model.Method
import sttp.tapir.model.{ConnectionInfo, ServerRequest}

import scala.jdk.CollectionConverters._

private[httpapi] class HttpApiServerRequest(event: APIGatewayV2HTTPEvent) extends ServerRequest {
  override def method: Method = Method(event.getRequestContext.getHttp.getMethod.toUpperCase)

  override def protocol: String = event.getRequestContext.getHttp.getProtocol

  override def uri: URI = new URI(
    s"https://${event.getRequestContext.getDomainName}${event.getRawPath}?${event.getRawQueryString}"
  )

  override def connectionInfo: ConnectionInfo = ConnectionInfo(
    local = None,
    remote = Some(InetSocketAddress.createUnresolved(event.getRequestContext.getHttp.getSourceIp, 0)),
    secure = Some(true)
  )

  override lazy val headers: Seq[(String, String)] = event.getHeaders.asScala.toList

  override def header(name: String): Option[String] = event.getHeaders.getIgnoreCase(name)
}

