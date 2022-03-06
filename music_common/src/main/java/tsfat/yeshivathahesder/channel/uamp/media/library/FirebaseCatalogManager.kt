package tsfat.yeshivathahesder.channel.uamp.media.library

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.tasks.await
import tsfat.yeshivathahesder.channel.uamp.R
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

/**
 * Attempts to download the catalog from firebase
 *
 * @param catalogUri URI to attempt to download the catalog form.
 * @return The catalog downloaded, or an empty catalog if an error occurred.
 */
suspend fun downloadCatalogSDK(context: Context): Catalog? {
    val db = Firebase.firestore

    try {
        val res = db.collection(context.getString(R.string.firebase_catalog_collection))
            .document(context.getString(R.string.firebase_catalog_document)).get().await()

        val catalog = res.toObject<Catalog>()
        return catalog ?: Catalog(emptyList())
    } catch (exception: FirebaseFirestoreException) {
        Log.w(
            TAG,
            "Seems like Firestore is unreachable via SDK. Exception: "
                    + exception.stackTraceToString()
        )
    }

    return null
}

fun downloadCatalogREST(context: Context): Catalog? {
    var res: Catalog? = null

    val url = URL(
        context.getString(
            R.string.firebase_rest_template,
            context.getString(R.string.firebase_catalog_collection),
            context.getString(R.string.firebase_catalog_document),
            context.getString(R.string.firebase_rest_api_key),
        )
    )

    val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
    try {
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        // add package name to request header
        val packageName: String = context.packageName
        connection.setRequestProperty("X-Android-Package", packageName)
        // add SHA certificate to request header
        val sig = DriveCatalogHelper.provideApplicationSignature(context)
        connection.setRequestProperty("X-Android-Cert", sig[0])
        connection.requestMethod = "GET"

        val reader = BufferedReader(InputStreamReader(connection.inputStream))
        res = restToCatalog(JsonParser.parseReader(reader).asJsonObject)
    } catch (e: Exception) {
        Log.w(TAG, "Can't connect to Firebase: ${e.stackTraceToString()}")
    } finally {
        connection.disconnect()
    }

    return res
}

private fun restToCatalog(json: JsonObject): Catalog {
    val items: MutableList<CatalogItem> = mutableListOf()

    val fields = json["fields"].asJsonObject
    val lessons = fields["lessons"].asJsonObject
    val values = lessons["arrayValue"].asJsonObject["values"].asJsonArray

    for (value in values) {
        items.add(restToCatalogItem(value.asJsonObject))
    }
    return Catalog(items)
}

private fun restToCatalogItem(json: JsonObject): CatalogItem {
    val mapValue = json["mapValue"].asJsonObject
    val fields = mapValue["fields"].asJsonObject
    val createdTime = fields["createdTime"].asJsonObject["stringValue"].asString
    val id = fields["id"].asJsonObject["stringValue"].asString
    val title = fields["title"].asJsonObject["stringValue"].asString
    val folder = fields["folder"].asJsonObject["stringValue"].asString

    return CatalogItem(id, title, folder, createdTime)
}

data class Catalog(
    val lessons: List<CatalogItem> = emptyList()
)

@Suppress("unused")
data class CatalogItem(
    val id: String = "", val title: String = "",
    val folder: String = "", val createdTime: String = ""
) {
    @Transient
    var source: String = ""

    @Transient
    var image: String = defaultAudioUri
}

const val defaultAudioUri =
    "https://yhtsfat.org.il/wp-content/uploads/2018/08/%D7%9C%D7%95%D7%92%D7%95-%D7%91%D7%90%D7%99%D7%9B%D7%95%D7%AA-%D7%99%D7%A9%D7%99%D7%91%D7%94-225x300.png"


private const val TAG = "FirebaseCatalogManager"