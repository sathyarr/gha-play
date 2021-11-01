package controllers

import org.apache.http.client.methods.HttpPost
import org.apache.http.client.utils.URIBuilder
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.json.{JSONArray, JSONObject}

import java.net.URI
import scala.collection.JavaConverters.iterableAsScalaIterableConverter
import scala.collection.mutable.ArrayBuffer

class GraphData(bodyParts: Array[String]) {
  private val mClient = HttpClients.createDefault()
  private var mURI: URI = _
  private var mReady = false

  def loadModel(url: String): Unit = {
    if (!mReady) {
      if (url.isEmpty) throw new RuntimeException("Server URL cannot be empty or unconfigured.")

      // build the uri object for future http requests.
      mURI = new URIBuilder(url).build()
      mReady = true
    }
    require(mReady, "Server is not initiated")
  }

  def getSectionName(input: String): String = {
    val inputSplit = input.split('/')
    input.split('/').foreach(x => {
      if(x.contains("_section")) {
        return x
      }
    })
    inputSplit.last
  }

  def getBeautifiedBodyPartText(input: String): String = {
    bodyParts.foreach(b => {
      if(b.toLowerCase == input.toLowerCase) {
        return b
      }
    })
    "UNKNOWN-BODYPART"
  }

  def getHighLevelDetail(input: JSONObject): String = {
    if(input.has("relations")) {
      val relatedArray = input.getJSONArray("relations").asScala.map(r => {
        r.asInstanceOf[JSONObject].get("text")
      })
      relatedArray.slice(0, 2).mkString(", ")
    }
    else {
      ""
    }
  }

  def filterRequiredData(data: String) = {
    val jsonData = new JSONObject(data)

    val lowLevelDetail = new JSONObject()
    val highLevelDetail = new JSONObject()
    val systemDetail = new JSONObject()

    val topk = 5
    var filled = 0

    val entities = jsonData.getJSONArray("entities")
    entities.asScala.foreach(entity => {
      val entityJsonObject = entity.asInstanceOf[JSONObject]
      if(entityJsonObject.getString("type") == "bodypart") {
        val bodyPartText = entityJsonObject.getString("text").toLowerCase
        if(bodyParts.map(_.toLowerCase).contains(bodyPartText)) { // TODO: can check for substring

          val beautifiedBodyPart = getBeautifiedBodyPartText(bodyPartText)

          val actualSectionName = getSectionName(entityJsonObject.getString("sectionName"))

          if(filled <= topk) {
            lowLevelDetail.put(beautifiedBodyPart, entityJsonObject.getDouble("score"))
            highLevelDetail.put(beautifiedBodyPart, getHighLevelDetail(entityJsonObject))
            filled += 1
          }
          if(true || actualSectionName.contains("_section")) {
            val randomNumber = new scala.util.Random().nextInt( (1 - 0) + 1 )
            val neutrality = if(randomNumber == 0) "negative" else "positive"

            if(systemDetail.has(actualSectionName)) {
              val existing = systemDetail.getJSONArray(actualSectionName)
              val firstObject = existing.getJSONObject(0) // first object should exist always
              val firstObjectNeutrality = firstObject.getString("neutrality")

              var added = false
              if(firstObjectNeutrality == neutrality) {
                // add the bodypart here
                firstObject.getJSONArray("bodyParts").put(beautifiedBodyPart)
                added = true
              }

              // second object may/not exist
              if(!added) {
                if(existing.length() == 2) {
                  val secondObject = existing.getJSONObject(1)
                  val secondObjectNeutrality = secondObject.getString("neutrality")
                  if (secondObjectNeutrality == neutrality) { // must be
                    // add the bodypart here
                    secondObject.getJSONArray("bodyParts").put(beautifiedBodyPart)
                  }
                }
                else {
                  val newJsonObject = new JSONObject()
                  newJsonObject.put("bodyParts", new JSONArray().put(beautifiedBodyPart))
                  newJsonObject.put("neutrality", neutrality)
                  existing.put(newJsonObject)
                }
              }

              //              systemDetail.put(actualSectionName, existing.put())
            }
            else {
              val newJsonObject = new JSONObject()
              newJsonObject.put("bodyParts", new JSONArray().put(beautifiedBodyPart))
              newJsonObject.put("neutrality", neutrality)
              systemDetail.put(actualSectionName, new JSONArray().put(newJsonObject))
            }
          }
        }
      }
    })

    (systemDetail, lowLevelDetail, highLevelDetail)
    //    (foundBodyParts.slice(0, 5))
  }

  def parse(data: String): (JSONObject, JSONObject, JSONObject) = {
    require(mReady, "Server is not initiated")

    val request = new HttpPost(mURI)
    request.setHeader("Content-Type", "text/plain; charset=UTF-8")
    request.setEntity(new StringEntity(data, "UTF-8"))

    request.setHeader("deep-parsing", "true")
    request.setHeader("phi-parsing", "true")
    request.setHeader("canon", "true")
    request.setHeader("post-processing", "false")
    request.setHeader("section-parsing", "true")
    request.setHeader("response-type", "json")

    val result = mClient.execute(request)
    val response = EntityUtils.toString(result.getEntity, "UTF-8")

    filterRequiredData(response)
  }
}
