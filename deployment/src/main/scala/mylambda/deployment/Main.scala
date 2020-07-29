package mylambda.deployment

import mylambda.BookFunction

object Main extends App {

  HttpApiCdkServer.deploy(BookFunction, "my-bookshop")

}
