# Serverless Tapir

This repo shows how to run the [Tapir](https://github.com/softwaremill/tapir) http application with the AWS Lambda and Http API. 
The deployment is done using the [AWS CDK](https://github.com/aws/aws-cdk) framework in a fully automated way.

This repo accompanies the article at https://melgenek.github.io/serverless-tapir.

## Building and deploying the application
Building the app
```
docker build -t serverless-tapir .
```
Deploying the app. The AWS credentials are taken from the `~/.aws/credentials` file in this case.
```
docker run --rm -it -v ~/.aws/:/root/.aws:ro serverless-tapir:latest
```
When the application is not used any more, all the resources can be deleted with this command:
```
docker run --rm -it -v ~/.aws/:/root/.aws:ro serverless-tapir:latest cdk destroy
```
