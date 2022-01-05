package tsfat.yeshivathahesder.channel.uamp.media.library

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import tsfat.yeshivathahesder.channel.uamp.R
import java.io.IOException
import java.net.UnknownHostException
import java.util.*

/**
 * Attempts to download the catalog from firebase
 *
 * @param catalogUri URI to attempt to download the catalog form.
 * @return The catalog downloaded, or an empty catalog if an error occurred.
 */
@Throws(IOException::class)
suspend fun downloadCatalog(context: Context): Pair<DriveCatalog, Boolean> {
    val db = Firebase.firestore

    val res = db.collection(context.getString(R.string.firebase_catalog_collection))
        .document(context.getString(R.string.firebase_catalog_document)).get().await()

    val catalog = res.toObject<DriveCatalog>()

    return Pair(catalog ?: DriveCatalog(emptyList()), res.metadata.isFromCache)
}

/**
 * Attempts to download the current catalog from Drive using [downloadCatalogRecursive],
 * and then upload it to firebase.
 */
@Throws(IOException::class)
suspend fun uploadCatalog(context: Context): DriveCatalog? {
    val uuid = UUID.randomUUID().toString()
    val db = Firebase.firestore

    val lockRef = db.collection(context.getString(R.string.firebase_catalog_collection))
        .document(context.getString(R.string.firebase_catalog_lock_document))
    Log.d(TAG, "Trying to get firebase lock.")

    val gotLock = db.runTransaction { transaction ->
        val lockVal = transaction.get(lockRef).toObject<FirebaseLock>()

        if (lockVal?.uuid == uuid) {
            return@runTransaction true
        }
        if (lockVal == null || Date().time - (lockVal?.timestamp?.time ?: 0) > waitTime) {
            transaction.set(lockRef, FirebaseLock(uuid))
            return@runTransaction true
        }
        return@runTransaction false
    }.await() ?: false

    if (!gotLock) {
        Log.d(TAG, "Did not get firebase lock. Database not uploaded.")
        return null
    }
    Log.d(TAG, "Got firebase lock. Building drive catalog.")

    return withContext(Dispatchers.IO) {
        val catalog: DriveCatalog
        try {
            catalog = downloadCatalogRecursive(context)
        } catch (e: IOException) {
            Log.e(TAG, e.stackTraceToString())
            return@withContext null
        } catch (e: UnknownHostException) {
            Log.e(TAG, e.stackTraceToString())
            return@withContext null
        }

        val catalogRef = db.collection(context.getString(R.string.firebase_catalog_collection))
            .document(context.getString(R.string.firebase_catalog_document))

        val uploaded = db.runTransaction { transaction ->
            val lockVal = transaction.get(lockRef).toObject<FirebaseLock>()
            if (lockVal?.uuid != uuid) return@runTransaction false

            transaction.set(catalogRef, catalog)
            transaction.set(lockRef, FirebaseLock(uuid))

            return@runTransaction true
        }.await() ?: false
        if (uploaded) {
            Log.d(TAG, "Uploaded catalog to firebase.")
        }
        return@withContext catalog
    }
}

private data class FirebaseLock(val uuid: String = "", @ServerTimestamp var timestamp: Date? = null)

private const val TAG = "FirebaseCatalogManager"
private const val waitTime = 5 * 60 * 1000