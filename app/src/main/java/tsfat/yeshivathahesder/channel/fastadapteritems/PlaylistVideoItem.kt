package tsfat.yeshivathahesder.channel.fastadapteritems

import tsfat.yeshivathahesder.channel.R
import tsfat.yeshivathahesder.channel.utils.DateTimeUtils
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import coil.api.load
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import tsfat.yeshivathahesder.channel.model.ItemBase
import tsfat.yeshivathahesder.channel.model.VideoItem

class PlaylistVideoItem(val playlistItem: ItemBase?) :
    AbstractItem<PlaylistVideoItem.PlaylistVideoViewHolder>() {

    override val layoutRes: Int
        get() = R.layout.item_playlist_video

    override val type: Int
        get() = R.id.fastadapter_playlist_video_item_id

    override fun getViewHolder(v: View): PlaylistVideoViewHolder {
        return PlaylistVideoViewHolder(v)
    }


    class PlaylistVideoViewHolder(private var view: View) :
        FastAdapter.ViewHolder<PlaylistVideoItem>(view) {

        private val thumbnail: AppCompatImageView =
            view.findViewById(R.id.ivThumbnailPlaylistVideoItem)
        private val videoTitle: AppCompatTextView = view.findViewById(R.id.tvTitlePlaylistVideoItem)
        private val videoPublishedAt: AppCompatTextView =
            view.findViewById(R.id.tvTimePublishedPlaylistVideoItem)

        override fun bindView(item: PlaylistVideoItem, payloads: List<Any>) {
            val playlistItem: ItemBase = item.playlistItem!!
            if (playlistItem is VideoItem) {
                playlistItem.snippet.let {
                    thumbnail.load(it.thumbnails.standard?.url ?: it.thumbnails.high.url)
                    videoTitle.text = it.title

                }
                videoPublishedAt.text =
                    DateTimeUtils.getTimeAgo(playlistItem.contentDetails.videoPublishedAt)
            }
        }

        override fun unbindView(item: PlaylistVideoItem) {
            thumbnail.setImageDrawable(null)
            videoTitle.text = null
            videoPublishedAt.text = null
        }
    }
}