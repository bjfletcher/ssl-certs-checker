package controllers

import java.io.IOException
import java.net.URL
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection

import play.api.libs.iteratee.{Concurrent, Iteratee}
import play.api.libs.json.Json
import play.api.mvc.{WebSocket, Action, Controller}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global // this is necessary for futures
import scala.io.Source

object Application extends Controller {

  case class Urls(urls: List[String])
  implicit val readsUrls = Json.reads[Urls]

  def urls: List[String] = {
    Json.parse(Source.fromFile("urls.json").mkString).as[Urls].urls
  }

  def index = Action { implicit request =>
    Ok(views.html.index(request))
  }

  def feed() = WebSocket.using[String] { implicit request =>
    val in = Iteratee.ignore[String]
    val out = Concurrent.unicast[String](onStart = channel => {
      for (url <- urls) Future {
        channel.push(Json.obj("url" -> url).toString)
        val result = try {
          val conn = new URL("https://" + url).openConnection()
          conn.connect()
          conn match {
            case https: HttpsURLConnection => {
              https.getServerCertificates.find(_.isInstanceOf[X509Certificate]) match {
                case Some(cert: X509Certificate) => Json.obj("expiry" -> cert.getNotAfter.getTime)
                case _ => Json.obj("error" -> "No certificate found")
              }
            }
            case _ => Json.obj("error" -> "URL's HTTPS is not secured")
          }
        } catch {
          case e: IOException => Json.obj("error" -> e.getLocalizedMessage.replaceFirst("^[^ ]+.[^ ]+: *", "")) // remove any class reference in error message
        }
        channel.push((Json.obj("url" -> url) ++ result).toString)
      }
    })
    (in, out)
  }

}