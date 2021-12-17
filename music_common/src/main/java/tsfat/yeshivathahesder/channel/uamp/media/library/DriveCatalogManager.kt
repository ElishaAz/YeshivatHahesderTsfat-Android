package tsfat.yeshivathahesder.channel.uamp.media.library

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.ServerTimestamp
import com.google.gson.Gson
import tsfat.yeshivathahesder.channel.R
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList

/**
 * Attempts to create a catalog from a google drive folder,
 * sending requests recursively for every folder.
 *
 * @param context the android context for resolving string resources
 * @param rootID the root folder's id
 * @return The catalog downloaded, or an empty catalog if an error occurred.
 */
@Throws(IOException::class)
fun downloadCatalogRecursive(context: Context): DriveCatalog {
    val startTime = System.currentTimeMillis()

    var totalQueryTime = 0.0
    var queryCount = 0

    val folders: Queue<DriveQuery> = LinkedList()
    val files: MutableList<DriveMusic> = ArrayList<DriveMusic>()

    val rootQueryStartTime = System.currentTimeMillis()
    folders.add(
        queryDrive(
            getQuery(
                context,
                context.getString(R.string.google_drive_root_id)
            )
        )
    )
    totalQueryTime += (System.currentTimeMillis() - rootQueryStartTime) / 1000.0
    queryCount++

    while (folders.isNotEmpty()) {
        val folder = folders.poll()
        folder ?: break
        for (file in folder.files) {
            if (file.mimeType.equals("application/vnd.google-apps.folder")) {

                val queryStartTime = System.currentTimeMillis()
                val query = queryDrive(getQuery(context, file.id))
                totalQueryTime += (System.currentTimeMillis() - queryStartTime) / 1000.0
                queryCount++

                query.name = file.name
                folders.add(query)
            } else if (file.mimeType.startsWith("audio/")) {
                files.add(queryToMusic(file, folder.name))
            } else {
                Log.d("DriveSource", "Unreadable mime type: " + file.mimeType)
            }
        }
    }

    val ret = DriveCatalog(files.sortedBy { it.createdTime })

    val time = (System.currentTimeMillis() - startTime) / 1000.0
    val queryAverage = totalQueryTime / queryCount
    Log.d(
        "DriveSource", "Downloaded List. Took $time seconds.\n" +
                "Average query time: $queryAverage. Query count: $queryCount."
    )
    return ret
}

/**
 * Attempts to create a catalog from a google drive folder,
 * sending one request for every layer. Currently not working.
 *
 * @param context the android context for resolving string resources
 * @param rootID the root folder's id
 * @return The catalog downloaded, or an empty catalog if an error occurred.
 */
@Throws(IOException::class)
fun downloadCatalogLayers(context: Context, rootID: String): DriveCatalog {
    val startTime = System.currentTimeMillis()

    val files: MutableList<DriveMusic> = ArrayList<DriveMusic>()
    val folders: MutableMap<String, DriveQueryItem> = mutableMapOf()
    val folderIds: MutableList<String> = ArrayList<String>()

    val firstQueryStart = System.currentTimeMillis()
    val root = queryDrive(getQuery(context, rootID))
    val firstQueryTime = (System.currentTimeMillis() - firstQueryStart) / 1000.0

    for (file in root.files) {
        if (file.mimeType.equals("application/vnd.google-apps.folder")) {
            folderIds.add(file.id)
            folders.put(file.id, file)
        } else if (file.mimeType.startsWith("audio/")) {
            files.add(queryToMusic(file, root.name))
        } else {
            //                    Log.d("DriveSource", "Unreadable mime type: " + file.mimeType)
        }
    }
    val secondQueryStart = System.currentTimeMillis()
    val filesQuery = queryDrive(getQuery(context, folderIds))
    val secondQueryTime = (System.currentTimeMillis() - secondQueryStart) / 1000.0

    for (file in filesQuery.files) {
        if (file.mimeType.startsWith("audio/")) {
            var parentName: String = "null"
            for (parent in file.parents) {
                val folder = folders.get(parent)
                if (folder != null) {
                    parentName = folder.name
                    break
                }
            }
            files.add(queryToMusic(file, parentName))
        }
    }
    val ret = DriveCatalog(files.sortedBy { it.createdTime })

    val time = (System.currentTimeMillis() - startTime) / 1000.0
    Log.d(
        "DriveSource", "Downloaded List. Took $time seconds.\n" +
                "First query time: $firstQueryTime.\n" +
                "Second query time: $secondQueryTime."
    )
    return ret
}

fun queryDrive(uri: String): DriveQuery {
    val catalogConn = URL(uri)
    val reader = BufferedReader(InputStreamReader(catalogConn.openStream()))
    return Gson().fromJson(reader, DriveQuery::class.java)
}

fun getQuery(context: Context, id: String): String {
    return context.getString(
        R.string.google_drive_query_template,
        "'$id' in parents",
        context.getString(R.string.drive_api_key)
    )
}

fun getQuery(context: Context, ids: List<String>): String {
    val sb = StringBuilder()
    var first = true
    for (id in ids) {
        if (!first) sb.append(" or ") else first = false
        sb.append("'$id' in parents")
    }
    return context.getString(
        R.string.google_drive_query_template,
        sb.toString(),
        context.getString(R.string.drive_api_key)
    )
}

fun fileIdToUri(context: Context, id: String): String {
//        val template = if (isFolder) context.getString(R.string.google_drive_folder_template)
    return context.getString(
        R.string.google_drive_link_template,
        id,
        context.getString(R.string.drive_api_key)
    )
//        return template.replace("{ID}", id)
//            .replace("{KEY}", context.getString(R.string.drive_api_key))
}

class DriveQuery {
    //    var kind: String = ""
//    var incompleteSearch: Boolean = false
    var files: List<DriveQueryItem> = emptyList()

    @Transient
    var name: String = "root"
}

class DriveQueryItem {
    var id: String = ""
    var name: String = ""
    var mimeType: String = ""
    var createdTime: String = ""
    var parents: List<String> = emptyList()
}

fun queryToMusic(item: DriveQueryItem, parentName: String): DriveMusic {
    return DriveMusic(item.id, item.name, parentName, item.createdTime)
}

data class DriveCatalog(
    val music: List<DriveMusic> = emptyList(),
    @ServerTimestamp
    var timestamp: Date? = null
)

@Suppress("unused")
data class DriveMusic(
    val id: String = "", val title: String = "",
    val folder: String = "", val createdTime: String = ""
) {
    @Transient
    var source: String = ""

    @Transient
    var image: String =
        "https://yhtsfat.org.il/wp-content/uploads/2018/08/%D7%9C%D7%95%D7%92%D7%95-%D7%91%D7%90%D7%99%D7%9B%D7%95%D7%AA-%D7%99%D7%A9%D7%99%D7%91%D7%94-225x300.png"
}
