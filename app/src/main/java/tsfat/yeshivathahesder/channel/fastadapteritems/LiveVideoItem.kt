package tsfat.yeshivathahesder.channel.fastadapteritems

import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import coil.api.load
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import tsfat.yeshivathahesder.channel.R
import tsfat.yeshivathahesder.channel.model.ItemBase
import tsfat.yeshivathahesder.channel.model.SearchedList
import tsfat.yeshivathahesder.channel.model.VideoItem
import tsfat.yeshivathahesder.channel.uamp.AudioItem
import tsfat.yeshivathahesder.channel.utils.DateTimeUtils

class LiveVideoItem(val videoItem: SearchedList.Item) :
    AbstractItem<LiveVideoItem.LiveVideoViewHolder>() {

    init {

    }

    override val layoutRes: Int
        get() = R.layout.item_live_video

    override val type: Int
        get() = R.id.fastadapter_item

    override fun getViewHolder(v: View): LiveVideoViewHolder {
        return LiveVideoViewHolder(v)
    }

    class LiveVideoViewHolder(view: View) :
        FastAdapter.ViewHolder<LiveVideoItem>(view) {

        private val thumbnail: AppCompatImageView = view.findViewById(R.id.ivThumbnailHomeItem)
        private val mediaTitle: AppCompatTextView = view.findViewById(R.id.tvTitleHomeItem)
        private val mediaPublishedAt: AppCompatTextView =
            view.findViewById(R.id.tvTimePublishedHomeItem)

        override fun bindView(item: LiveVideoItem, payloads: List<Any>) {
            val mediaItem = item.videoItem
            mediaItem.snippet.let {
                thumbnail.load(it.thumbnails.resUrl)
                mediaTitle.text = it.title
            }
            mediaPublishedAt.text =
                DateTimeUtils.getTimeAgo(mediaItem.publishedAt)
        }

        override fun unbindView(item: LiveVideoItem) {
            thumbnail.setImageDrawable(null)
            mediaTitle.text = null
            mediaPublishedAt.text = null
        }
    }
}