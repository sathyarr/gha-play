package controllers

import org.apache.http.client.methods.HttpPost
import org.apache.http.client.utils.URIBuilder
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.json.{JSONArray, JSONObject}

import java.net.URI
import scala.collection.JavaConverters.iterableAsScalaIterableConverter

class PHIData(priorityList: JSONArray) {
  private val mClient = HttpClients.createDefault()
  private var mURI: URI = _
  private var mReady = false
  private val kProviderNames = "providerNames"

  def loadModel(url: String): Unit = {
    if (!mReady) {
      if (url.isEmpty) throw new RuntimeException("Server URL cannot be empty or unconfigured.")

      // build the uri object for future http requests.
      mURI = new URIBuilder(url).build()
      mReady = true
    }
    require(mReady, "Server is not initiated")
  }

  def filterRequiredData(data: String): JSONObject = {
    val jsonData = new JSONObject(data)
    val resultData = new JSONObject()

    // TODO: handle providerNames complex data type
    var found = 0;
    priorityList.asScala.foreach { keyO =>
      val key = keyO.asInstanceOf[String]
      if(key == kProviderNames) {
        val pnArray = jsonData.getJSONArray(kProviderNames)
        if(pnArray.length() > 0) {
          val temp = pnArray.getJSONObject(0)
          resultData.put(key, temp.getString("name") + " " + temp.getString("qualification"))
          found += 1
        }
      }
      else if(jsonData.getString(key).nonEmpty) {
        resultData.put(key, jsonData.get(key))
        found += 1
      }

      if(found == 5) {
        return resultData
      }
    }
    resultData
  }

  def parse(data: String): JSONObject = {
    require(mReady, "Server is not initiated")

    val request = new HttpPost(mURI)
    request.setHeader("Content-Type", "text/plain; charset=UTF-8")
    request.setEntity(new StringEntity(data, "UTF-8"))

    val result = mClient.execute(request)
    val response = EntityUtils.toString(result.getEntity, "UTF-8")

    filterRequiredData(response)
  }
}
