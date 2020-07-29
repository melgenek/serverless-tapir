FROM hseeberger/scala-sbt:8u242_1.3.7_2.13.1

RUN curl -sL https://deb.nodesource.com/setup_14.x | bash -
RUN apt install -y nodejs
RUN npm install -g aws-cdk@1.54.0 && cdk --version

ADD . $HOME/serverless-tapir/
WORKDIR $HOME/serverless-tapir/

RUN sbt assembly

CMD ["cdk", "deploy"]
