package mylambda.deployment

import software.amazon.awscdk
import software.amazon.awscdk.core.{CfnOutput, Duration, RemovalPolicy, Stack}
import software.amazon.awscdk.services.apigatewayv2.{AddRoutesOptions, HttpApi, HttpMethod, LambdaProxyIntegration, PayloadFormatVersion}
import software.amazon.awscdk.services.iam.{Role, ServicePrincipal}
import software.amazon.awscdk.services.lambda
import software.amazon.awscdk.services.lambda.Code
import software.amazon.awscdk.services.logs.{LogGroup, RetentionDays}
import sttp.tapir.docs.openapi._
import sttp.tapir.server.httpapi.HttpApiFunction

import scala.jdk.CollectionConverters._


object HttpApiCdkServer {

  def deploy(httpApiFunction: HttpApiFunction, title: String): Unit = {
    val app = new awscdk.core.App()
    val stack = new Stack(app, s"$title-stack")

    val logGroup = LogGroup.Builder.create(stack, s"$title-log-group")
      .removalPolicy(RemovalPolicy.DESTROY)
      .logGroupName(s"/aws/lambda/$title")
      .retention(RetentionDays.ONE_DAY)
      .build()

    val role = Role.Builder.create(stack, s"$title-role")
      .roleName(title)
      .assumedBy(ServicePrincipal.Builder.create("lambda.amazonaws.com").build())
      .build()
    logGroup.grantWrite(role)

    val function = lambda.Function.Builder.create(stack, s"$title-lambda")
      .role(role)
      .memorySize(192)
      .timeout(Duration.seconds(30))
      .functionName(title)
      .runtime(lambda.Runtime.JAVA_8)
      .handler(s"${httpApiFunction.getClass.getName.replace("$", "")}::onEvent")
      .code(Code.fromAsset("lambda/target/scala-2.13/assembly.jar"))
      .build()

    val api = HttpApi.Builder.create(stack, s"$title-api")
      .apiName(title)
      .build()

    val integration = LambdaProxyIntegration.Builder.create()
      .handler(function)
      .payloadFormatVersion(PayloadFormatVersion.VERSION_2_0)
      .build()
    apiRoutes(httpApiFunction, integration).foreach(api.addRoutes)

    CfnOutput.Builder.create(stack, "api-url").exportName("url").value(api.getUrl).build()
    app.synth()
  }

  private def apiRoutes(httpApiFunction: HttpApiFunction, integration: LambdaProxyIntegration): List[AddRoutesOptions] = {
    val openAPI = httpApiFunction.serverEndpoints.map(_.endpoint).toOpenAPI("any name", "v1")
    openAPI.paths
      .map { case (path, pathItem) =>
        val methods =
          pathItem.get.map(_ => HttpMethod.GET) ++
            pathItem.post.map(_ => HttpMethod.POST) ++
            pathItem.delete.map(_ => HttpMethod.DELETE)
        AddRoutesOptions.builder()
          .methods(methods.toList.asJava)
          .path(path)
          .integration(integration)
          .build()
      }
      .toList
  }

}
