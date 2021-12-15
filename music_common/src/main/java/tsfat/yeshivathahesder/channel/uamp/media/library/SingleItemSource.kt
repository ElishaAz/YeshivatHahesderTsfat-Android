package tsfat.yeshivathahesder.channel.uamp.media.library

import android.net.Uri
import android.support.v4.media.MediaMetadataCompat
import java.lang.IndexOutOfBoundsException

class SingleItemSource() : AbstractMusicSource() {
    private var item: MediaMetadataCompat? = null

    fun setItem(item: MediaMetadataCompat) {
        this.item = item
    }

    override suspend fun load() {

    }

    private class SingleIterator<T>(private val item: T) : Iterator<T> {
        private var loaded: Boolean = false;
        override fun hasNext(): Boolean = !loaded

        override fun next(): T {
            if (loaded) throw IndexOutOfBoundsException()

            loaded = true
            return item
        }

    }

    override fun iterator(): Iterator<MediaMetadataCompat> =
        SingleIterator<MediaMetadataCompat>(item!!)


}

val SINGLE_MEDIA_METADATA_EXTRA = "tsfat.yeshivathahesder.cannel.single_media_metadata"