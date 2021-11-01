package controllers

import scala.collection.JavaConverters._
import org.json.JSONObject

import javax.inject._
import play.api._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents)(implicit exec: ExecutionContext) extends AbstractController(cc) {

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */

  def parseEHR(): Action[String] = Action.async(parse.tolerantText) { request: Request[String] =>
    val inputData: String = request.body

    val config = new JSONObject(scala.io.Source.fromFile("conf/config.json").getLines().mkString("\n"))

    val phiData = new PHIData(config.getJSONArray("phi-data-lookup-keys"))
    phiData.loadModel(config.getJSONObject("urls").getString("phi"))
    val phiResult = phiData.parse(inputData)

    val bodyParts = config.getJSONObject("bodypart-mesh-mapping").keySet().asScala.toArray
    println(bodyParts.mkString(", "))

    val graphData = new GraphData(bodyParts)
    graphData.loadModel(config.getJSONObject("urls").getString("micrograph"))
    val graphResult = graphData.parse(inputData)
    val systemResult = graphResult._1
    val lowLevelResult = graphResult._2
    val highLevelResult = graphResult._3

    val allBodyPartMeshMapping = config.getJSONObject("bodypart-mesh-mapping")
    val topkBodyPartMeshMapping = new JSONObject()
    lowLevelResult.keySet().asScala.foreach(k => {
      topkBodyPartMeshMapping.put(k, allBodyPartMeshMapping.get(k))
    })

    val result = new JSONObject()
    result.put("allbpmmapping", allBodyPartMeshMapping)
    result.put("topkbpmmapping", topkBodyPartMeshMapping)
    result.put("phi-data", phiResult)
    result.put("system-data", systemResult)
    result.put("low-data", lowLevelResult)
    result.put("high-data", highLevelResult)

    Future {
      Ok(result.toString(4)).as("application/json")
    }(exec)
  }

  def parseEHRSynthetic(): Action[String] = Action.async(parse.tolerantText) { request: Request[String] =>
    val result = new JSONObject(scala.io.Source.fromFile("synthetic-new.json").getLines().mkString("\n"))

    Future {
      Ok(result.toString(4)).as("application/json")
    }(exec)
  }
}
