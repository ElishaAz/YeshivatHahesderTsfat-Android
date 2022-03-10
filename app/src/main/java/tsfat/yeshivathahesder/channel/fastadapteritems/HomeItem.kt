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

class HomeItem(val playlistItem: ItemBase?) :
    AbstractItem<HomeItem.HomeViewHolder>() {

    init {

    }

    override val layoutRes: Int
        get() = R.layout.item_home

    override val type: Int
        get() = R.id.fastadapter_home_item_id

    override fun getViewHolder(v: View): HomeViewHolder {
        return HomeViewHolder(v)
    }

    class HomeViewHolder(private var view: View) : FastAdapter.ViewHolder<HomeItem>(view) {

        private val thumbnail: AppCompatImageView = view.findViewById(R.id.ivThumbnailHomeItem)
        private val mediaTitle: AppCompatTextView = view.findViewById(R.id.tvTitleHomeItem)
        private val mediaPublishedAt: AppCompatTextView =
            view.findViewById(R.id.tvTimePublishedHomeItem)

        override fun bindView(item: HomeItem, payloads: List<Any>) {
            val mediaItem: ItemBase = item.playlistItem!!
            if (mediaItem is VideoItem) {
                mediaItem.snippet.let {
                    thumbnail.load(it.thumbnails.resUrl)
                    mediaTitle.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                         Html.fromHtml(it.title, Html.FROM_HTML_MODE_COMPACT)
                    }else{
                        Html.fromHtml(it.title)
                    }
                }
                mediaPublishedAt.text =
                    DateTimeUtils.getTimeAgo(mediaItem.contentDetails.videoPublishedAt)
            } else if (mediaItem is AudioItem) {
                mediaTitle.text = mediaItem.fullTitle

                thumbnail.load(mediaItem.albumArtUri)

                mediaPublishedAt.text = if (mediaItem.publishedAt.isBlank()) ""
                else DateTimeUtils.getTimeAgo(mediaItem.publishedAt)
            }
        }

        override fun unbindView(item: HomeItem) {
            thumbnail.setImageDrawable(null)
            mediaTitle.text = null
            mediaPublishedAt.text = null
        }
    }
}