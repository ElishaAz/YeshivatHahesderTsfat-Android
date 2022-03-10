package tsfat.yeshivathahesder.channel.model

import timber.log.Timber
import tsfat.yeshivathahesder.channel.uamp.AudioItem
import java.lang.NullPointerException


/**
 * Model class that is returned when a Video Search API call is made.
 *
 * URL: https://www.googleapis.com/youtube/v3/search
 */
data class SearchedList<T : SearchedList.SearchItem>(
    val items: List<T>,
    val nextPageToken: String?
) {
    data class Item(
        val id: Id,
        val snippet: Snippet
    ) : SearchItem() {
        data class Id(
            val videoId: String
        )

        data class Snippet(
            val publishedAt: String,
            val thumbnails: Thumbnails,
            val title: String
        ) {
            data class Thumbnails(
                val default: Default?,
                val high: High?,
                val medium: Medium?,
                val standard: Standard?

            ) {
                data class Default(
                    val height: Int,
                    val url: String,
                    val width: Int
                )

                data class High(
                    val height: Int,
                    val url: String,
                    val width: Int
                )

                data class Medium(
                    val height: Int,
                    val url: String,
                    val width: Int
                )

                data class Standard(
                    val height: Int,
                    val url: String,
                    val width: Int
                )

                val resUrl
                    get() = standard?.url ?: high?.url ?: default?.url ?: medium?.url
                    ?: throw NullPointerException("No resolution!")
            }
        }

        @Transient
        private var grade: Double? = null

        override fun getGrade(query: String): Double =
            grade ?: search(query, snippet.title).also { grade = it }

        override val baseId: String = videoIdToBase(id.videoId)
        override val publishedAt: String
            get() = snippet.publishedAt
    }

    data class AudioSearchItem(val grade: Double, val item: AudioItem) : SearchItem() {
        override fun getGrade(query: String): Double = grade

        override val baseId: String
            get() = item.baseId
        override val publishedAt: String
            get() = item.publishedAt

    }

    abstract class SearchItem : ItemBase() {
        abstract fun getGrade(query: String = ""): Double
    }
}

fun search(query: String, text: String): Double {
    val words = query.split(" ").toSet()
    val matchingWords: MutableList<String> = mutableListOf()
    for (word in words) {
        if (text.contains(word)) {
            matchingWords.add(word)
        }
    }
    return matchingWords.size.toDouble() / words.size.toDouble()
}