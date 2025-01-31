package tsfat.yeshivathahesder.channel.fastadapteritems

import android.os.Build
import android.text.Html
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
import tsfat.yeshivathahesder.channel.uamp.AudioItem

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
                    thumbnail.load(it.thumbnails.resUrl)
                    videoTitle.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        Html.fromHtml(it.title, Html.FROM_HTML_MODE_COMPACT)
                    } else {
                        Html.fromHtml(it.title)
                    }

                }
                videoPublishedAt.text =
                    DateTimeUtils.getTimeAgo(playlistItem.contentDetails.videoPublishedAt)
            } else if (playlistItem is AudioItem) {
                thumbnail.load(playlistItem.albumArtUri)
                videoTitle.text = playlistItem.fullTitle
                videoPublishedAt.text = DateTimeUtils.getTimeAgo(playlistItem.publishedAt)
            }
        }

        override fun unbindView(item: PlaylistVideoItem) {
            thumbnail.setImageDrawable(null)
            videoTitle.text = null
            videoPublishedAt.text = null
        }
    }
}