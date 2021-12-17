package tsfat.yeshivathahesder.channel.repository

import android.util.Log
import androidx.lifecycle.Observer
import kotlinx.coroutines.delay
import retrofit2.Response
import timber.log.Timber
import tsfat.yeshivathahesder.channel.di.AudioConnector
import tsfat.yeshivathahesder.channel.model.*
import tsfat.yeshivathahesder.channel.uamp.AudioItem

class AudioRepository(private val audioConnector: AudioConnector) {
    private suspend fun lockAudioItemsBusy() {
        while (audioConnector.audioItems.value.isNullOrEmpty()) {
            Timber.d("Waiting for audio items...")
            delay(1000)
        }
    }

    private val lock = CountDownLatch(1)
    private var observer: Observer<List<AudioItem>>? = null
    private suspend fun lockAudioItemsLatch() {
        if (audioConnector.audioItems.value.isNullOrEmpty()) {
            if (observer == null) {
                observer = Observer {
                    if (!it.isNullOrEmpty())
                        lock.countDown()
                }
                audioConnector.audioItems.observeForever(observer!!)
            }
//        if (audioItemList == null || audioItemList.isEmpty()) {
//            Timber.d("HomeRepository", "Waiting for audio items...")
//            lock.withLock {
//                cond.await()
//            }
//            Timber.d("HomeRepository", "Audio items lock released")
//            audioItemList = audioConnector.audioItems.value
//            if (audioItemList == null)
//                Timber.d("HomeRepository", "No audio items!")
//        }
            Timber.d("Waiting for audio items...")
            lock.await()
            Timber.d("Audio items loaded")
        }
    }

    suspend fun waitForAudioItems() {
        lockAudioItemsBusy()
    }

    suspend fun getAudioItems(): List<AudioItem> {

        waitForAudioItems()

        val audioItemList: List<AudioItem>? = audioConnector.audioItems.value

        val allAudioItems: List<AudioItem> = audioItemList?.asReversed() ?: emptyList()
        return allAudioItems
    }

    suspend fun searchAudioItems(searchQuery: String): List<SearchedList.AudioSearchItem> {
        waitForAudioItems()
        val searchItems: MutableList<SearchedList.AudioSearchItem> = mutableListOf()
        val audioItems = audioConnector.audioItems.value?.asReversed() ?: return emptyList()

        for (item in audioItems) {

            val grade = search(searchQuery, item.fullTitle)

            if (grade > 0) {
                searchItems.add(SearchedList.AudioSearchItem(grade, item))
            }
        }
        return searchItems
    }
}

fun joinAudioAndVideoResults(
    response: Response<ItemList<VideoItem>>,
    audioItems: List<AudioItem>
): Response<ItemList<ItemBase>> {
    val body = response.body() ?: ItemList(null, null, emptyList())
    val newList =
        (audioItems + body.items).sortedByDescending { it.publishedAt }
//        mergeSortedLists(audioItems, body.items) { a, b -> a.publishedAt.compareTo(b.publishedAt) }

    if (response.isSuccessful) {
        return Response.success(
            ItemList<ItemBase>(
                body.nextPageToken,
                body.prevPageToken,
                newList
            )
        )
    } else return Response.error(response.errorBody()!!, response.raw())
}

fun joinAudioAndVideoSearchResults(
    response: Response<SearchedList<SearchedList.Item>>,
    audioItems: List<SearchedList.AudioSearchItem>,
    query: String
): Response<SearchedList<SearchedList.SearchItem>> {
    val body = response.body() ?: SearchedList(emptyList(), null)
    val newList =
        (audioItems + body.items).sortedByDescending {
            it.getGrade(query)
        }
//        mergeSortedLists(audioItems, body.items) { a, b -> a.publishedAt.compareTo(b.publishedAt) }

    if (response.isSuccessful) {
        return Response.success(
            SearchedList(
                newList,
                body.nextPageToken
            )
        )
    } else return Response.error(response.errorBody()!!, response.raw())
}

fun <T> mergeSortedLists(l1: List<T>, l2: List<T>, comparator: Comparator<T>): List<T> {
    val res = mutableListOf<T>()
    val iter1 = l1.iterator()
    val iter2 = l2.iterator()
    if (!iter1.hasNext()) return l2
    if (!iter2.hasNext()) return l1
    var val1 = iter1.next()
    var val2 = iter2.next()

    while (iter1.hasNext() && iter2.hasNext()) {
        val comp = comparator.compare(val1, val2)
        if (comp >= 0) {
            res.add(val1)
            val1 = iter1.next()
        }
        if (comp <= 0) {
            res.add(val2)
            val2 = iter2.next()
        }
    }
    do {
        res.add(val1)
    } while (iter1.hasNext().also { if (it) val1 = iter1.next() })
    do {
        res.add(val2)
    } while (iter2.hasNext().also { if (it) val1 = iter2.next() })

    return res
}