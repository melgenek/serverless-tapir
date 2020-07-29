package sttp.tapir.server.httpapi

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import sttp.tapir.RawBodyType

private[httpapi] object HttpApiRequestToRawBody {

  def apply[R](event: APIGatewayV2HTTPEvent, bodyType: RawBodyType[R]): R = {
    bodyType match {
      case RawBodyType.StringBody(_) => event.getBody
      case RawBodyType.ByteArrayBody => event.getBody.getBytes(StandardCharsets.UTF_8)
      case RawBodyType.ByteBufferBody => StandardCharsets.UTF_8.encode(event.getBody)
      case RawBodyType.InputStreamBody => new ByteArrayInputStream(event.getBody.getBytes(StandardCharsets.UTF_8))
      case RawBodyType.FileBody => throw new UnsupportedOperationException()
      case _: RawBodyType.MultipartBody => throw new UnsupportedOperationException()
    }
  }

}
