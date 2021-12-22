package tsfat.yeshivathahesder.channel.repository

import androidx.lifecycle.Observer
import kotlinx.coroutines.delay
import retrofit2.Response
import timber.log.Timber
import tsfat.yeshivathahesder.channel.di.AudioConnector
import tsfat.yeshivathahesder.channel.model.*
import tsfat.yeshivathahesder.channel.uamp.AudioItem
import tsfat.yeshivathahesder.channel.uamp.AudioPlaylist

class AudioRepository(private val audioConnector: AudioConnector) {
//    init {
//        audioConnector.audioItems.observeForever {
//            audioPageMap.clear()
//            audioPlaylistsPageMap.clear()
//        }
//    }

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


    private val audioPageMap: MutableList<Pair<String, Pair<Int, Int>>> = mutableListOf()

    suspend fun getAudioItems(
        pageToken: String?,
        nextPageToken: String?,
        lastPublishedAt: String
    ): List<AudioItem> {
        val mPageToken = pageToken ?: "start"
        val mNextPageToken = nextPageToken ?: "end"

        waitForAudioItems()
        val audioItems = audioConnector.audioItems.value?.asReversed() ?: emptyList()

        val res = getPage(
            audioPageMap,
            mNextPageToken,
            mPageToken,
            audioItems,
            lastPublishedAt
        ) { it.publishedAt }

        Timber.d(res.toString())

        return res

//        if (mNextPageToken == "end") {
//            val lastIndex = if (audioPageMap.isEmpty()) 0 else audioPageMap.last().second.second
//            audioPageMap.add(Pair(mPageToken, Pair(lastIndex, audioItems.size)))
//            return audioItems.subList(lastIndex, audioItems.size)
//        }
//
//        if (mPageToken == "start") {
//            assert(audioPageMap.isEmpty())
//            for ((index, item) in audioItems.withIndex()) {
//                if (item.publishedAt < lastPublishedAt) {
//                    audioPageMap.add(Pair(mPageToken, Pair(0, index)))
//                    return audioItems.subList(0, index)
//                }
//            }
//        }
//
//        for ((token, pair) in audioPageMap) {
//            if (token == pageToken)
//                return audioItems.subList(pair.first, pair.second)
//        }
//
//        val lastIndex = audioPageMap.last().second.second
//
//        var i = lastIndex
//        while (i < audioItems.size) {
//            if (audioItems[i].publishedAt < lastPublishedAt) {
//                audioPageMap.add(Pair(mPageToken, Pair(lastIndex, i)))
//                return audioItems.subList(0, i)
//            }
//            i++
//        }
//        return emptyList()
    }

    suspend fun searchAudioItems(searchQuery: String): List<SearchedList.AudioSearchItem> {
        waitForAudioItems()
        return searchAudioItemsNow(searchQuery)
    }

    @Synchronized
    private fun searchAudioItemsNow(searchQuery: String): List<SearchedList.AudioSearchItem> {
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

    private val audioPlaylistsPageMap: MutableList<Pair<String, Pair<Int, Int>>> = mutableListOf()

    suspend fun getAudioPlaylists(
        pageToken: String?,
        nextPageToken: String?,
        lastPublishedAt: String
    ): List<AudioPlaylist> {

        val mPageToken = pageToken ?: "start"
        val mNextPageToken = nextPageToken ?: "end"

        waitForAudioItems()
        val playlists = audioConnector.playlists.value ?: return emptyList()
        val res = getPage(
            audioPlaylistsPageMap,
            mNextPageToken,
            mPageToken,
            playlists,
            lastPublishedAt
        ) { it.publishedAt }

        Timber.d(res.toString())

        return res
    }

    @Synchronized
    private fun <T, R : Comparable<R>> getPage(
        pageMap: MutableList<Pair<String, Pair<Int, Int>>>,
        mNextPageToken: String,
        mPageToken: String,
        list: List<T>,
        lastMinVal: R,
        selector: (T) -> R
    ): List<T> {
        if (mNextPageToken == "end") {
            val lastIndex =
                if (pageMap.isEmpty()) 0 else pageMap.last().second.second
            pageMap.add(Pair(mPageToken, Pair(lastIndex, list.size)))
            return list.subList(lastIndex, list.size)
        }

        if (mPageToken == "start") {
            assert(pageMap.isEmpty())
            for ((index, item) in list.withIndex()) {
                if (selector(item) < lastMinVal) {
                    pageMap.add(Pair(mPageToken, Pair(0, index)))
                    return list.subList(0, index)
                }
            }
            pageMap.add(Pair(mPageToken, Pair(0, list.size)))
            return list
        }

        for ((token, pair) in pageMap) {
            return list.subList(pair.first, pair.second)
        }

        if (pageMap.isEmpty()){
            Timber.d(pageMap.toString() + ", " + mPageToken)
        }

        val lastIndex = pageMap.last().second.second

        var i = lastIndex
        while (i < list.size) {
            if (selector(list[i]) < lastMinVal) {
                pageMap.add(Pair(mPageToken, Pair(lastIndex, i)))
                return list.subList(lastIndex, i)
            }
            i++
        }

        return emptyList()
    }

    suspend fun getAudioPlaylist(id: String): List<AudioItem> {
        waitForAudioItems()
        val playlists = audioConnector.playlists.value ?: emptyList()
        for (playlist in playlists) {
            if (playlist.mediaId == id) {
                return playlist.items.asReversed()
            }
        }
        return emptyList()
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