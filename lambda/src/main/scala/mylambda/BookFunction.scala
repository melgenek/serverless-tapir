package mylambda

import io.circe.generic.auto._
import sttp.tapir._
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.httpapi.HttpApiFunction

import scala.util.{Failure, Success, Try}

object Endpoints {
  type Limit = Int
  type AuthToken = String
  case class BooksFromYear(genre: String, year: Int)
  case class Book(title: String)

  val bookListing =
    endpoint.get
      .in(("books" / path[String]("genre") / path[Int]).mapTo(BooksFromYear))
      .in(query[Limit]("limit").description("Maximum number of books to retrieve"))
      .in(header[AuthToken]("X-Auth-Token"))
      .errorOut(stringBody)
      .out(jsonBody[List[Book]])

  val getBooks: ServerEndpoint[(BooksFromYear, Limit, AuthToken), AuthToken, List[Book], Nothing, Try] =
    bookListing
      .serverLogic { case (year, limit, token) =>
        val inputs = s"Year: $year. Limit: $limit. Token: $token"
        println(inputs)
        Success(Right(List(Book(inputs))))
      }

  val getBook: ServerEndpoint[(Limit, AuthToken), AuthToken, Book, Nothing, Try] =
    endpoint.get
      .in("books" / path[Int]("id"))
      .in(header[AuthToken]("X-Auth-Token"))
      .errorOut(stringBody)
      .out(jsonBody[Book])
      .serverLogic { case (id, token) =>
        val inputs = s"Id: $id. Token: $token"
        println(inputs)
        Success(Right(Book(inputs)))
      }

  val createBook: ServerEndpoint[(AuthToken, Book), AuthToken, Book, Nothing, Try] =
    endpoint.post
      .in("books")
      .in(header[AuthToken]("X-Auth-Token"))
      .in(jsonBody[Book])
      .errorOut(stringBody)
      .out(jsonBody[Book])
      .serverLogic { case (token, book) =>
        val inputs = s"Token: $token. Book: $book"
        println(inputs)
        Success(Left(s"The book '$inputs' was not created"))
      }

  val deleteBook: ServerEndpoint[(Limit, AuthToken), AuthToken, Book, Nothing, Try] =
    endpoint.delete
      .in("books" / path[Int]("id"))
      .in(header[AuthToken]("X-Auth-Token"))
      .errorOut(stringBody)
      .out(jsonBody[Book])
      .serverLogic { case (id, token) =>
        val inputs = s"Id: $id. Token: $token"
        println(inputs)
        Failure(new NumberFormatException(s"An internal error. $inputs"))
      }

  val endpoints = List(
    Endpoints.getBooks,
    Endpoints.getBook,
    Endpoints.createBook,
    Endpoints.deleteBook
  )

}

object BookFunction extends HttpApiFunction {
  override val serverEndpoints = Endpoints.endpoints
}
