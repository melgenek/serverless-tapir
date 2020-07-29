package sttp.tapir.server.httpapi

import java.io.{File, InputStream}
import java.nio.ByteBuffer
import java.nio.charset.{Charset, StandardCharsets}

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse
import sttp.model.StatusCode
import sttp.tapir.internal.{ParamsAsAny, charset}
import sttp.tapir.server.internal.{EncodeOutputBody, EncodeOutputs, OutputValues}
import sttp.tapir.{CodecFormat, EndpointOutput, RawBodyType}

import scala.io.Source
import scala.jdk.CollectionConverters._
import scala.util.Using

private[httpapi] object OutputToHttpApiResponse {

  def apply(statusCode: StatusCode, body: String = ""): APIGatewayV2HTTPResponse = {
    APIGatewayV2HTTPResponse.builder
      .withIsBase64Encoded(false)
      .withStatusCode(statusCode.code)
      .withBody(body)
      .build()
  }

  def apply[O](defaultStatus: StatusCode, output: EndpointOutput[O], v: O): APIGatewayV2HTTPResponse = {
    val outputValues = encodeOutputs(output, ParamsAsAny(v), OutputValues.empty)
    val status = outputValues.statusCode.getOrElse(defaultStatus)
    val headers = outputValues.headers.toMap

    val builder = APIGatewayV2HTTPResponse.builder
      .withIsBase64Encoded(false)
      .withStatusCode(status.code)

    outputValues.body
      .map { body =>
        builder
          .withBody(body.entity)
          .withHeaders((headers + ("Content-Type" -> body.contentType)).asJava)
      }
      .getOrElse {
        builder.withBody("").withHeaders(headers.asJava)
      }
      .build()
  }

  private val encodeOutputs: EncodeOutputs[ResponseBody] =
    new EncodeOutputs[ResponseBody](new EncodeOutputBody[ResponseBody] {
      override def rawValueToBody(v: Any, format: CodecFormat, bodyType: RawBodyType[_]): ResponseBody =
        ResponseBody(
          rawValueToResponseEntity(bodyType.asInstanceOf[RawBodyType[Any]], v),
          formatToContentType(format, charset(bodyType))
        )

      override def streamValueToBody(v: Any, format: CodecFormat, charset: Option[Charset]): ResponseBody =
        throw new UnsupportedOperationException("Trying to read streaming body from a non-streaming request")

    })

  private def rawValueToResponseEntity[CF <: CodecFormat, R](bodyType: RawBodyType[R], r: R): String = {
    bodyType match {
      case RawBodyType.StringBody(charset) => new String(r.asInstanceOf[String].getBytes, charset)
      case RawBodyType.ByteArrayBody => new String(r.asInstanceOf[Array[Byte]], StandardCharsets.UTF_8)
      case RawBodyType.ByteBufferBody => StandardCharsets.UTF_8.decode(r.asInstanceOf[ByteBuffer]).toString
      case RawBodyType.InputStreamBody => Source.fromInputStream(r.asInstanceOf[InputStream]).mkString
      case RawBodyType.FileBody => Using.resource(Source.fromFile(r.asInstanceOf[File]))(_.mkString)
      case _: RawBodyType.MultipartBody => throw new UnsupportedOperationException()
    }
  }

  private def formatToContentType(format: CodecFormat, charset: Option[Charset]): String = {
    val mt = format.mediaType
    charset.map(c => mt.charset(c.toString)).getOrElse(mt).toString
  }

  private[this] final case class ResponseBody(entity: String, contentType: String)
}
