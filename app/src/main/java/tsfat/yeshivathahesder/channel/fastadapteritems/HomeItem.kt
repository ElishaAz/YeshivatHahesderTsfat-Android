package tsfat.yeshivathahesder.channel.fastadapteritems

import tsfat.yeshivathahesder.channel.R
import tsfat.yeshivathahesder.channel.model.PlaylistItemInfo
import tsfat.yeshivathahesder.channel.utils.DateTimeUtils
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import coil.api.load
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem

class HomeItem(val playlistItem: PlaylistItemInfo.ItemBase?) :
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
        private val videoTitle: AppCompatTextView = view.findViewById(R.id.tvTitleHomeItem)
        private val videoPublishedAt: AppCompatTextView =
            view.findViewById(R.id.tvTimePublishedHomeItem)

        override fun bindView(item: HomeItem, payloads: List<Any>) {
            val playlistItem: PlaylistItemInfo.ItemBase = item.playlistItem!!
            if (playlistItem is PlaylistItemInfo.VideoItem) {
                playlistItem.snippet.let {
                    thumbnail.load(it.thumbnails.standard?.url ?: it.thumbnails.high.url)
                    videoTitle.text = it.title
                }
                videoPublishedAt.text =
                    DateTimeUtils.getTimeAgo(playlistItem.contentDetails.videoPublishedAt)
            }
//            else if (playlistItem is PlaylistItemInfo.AudioItem) {
//                playlistItem.snippet.let {
//                    videoTitle.text = it.title
//                }
//                videoPublishedAt.text =
//                    DateTimeUtils.getTimeAgo(playlistItem.contentDetails.publicationDate)
//            }
        }

        override fun unbindView(item: HomeItem) {
            thumbnail.setImageDrawable(null)
            videoTitle.text = null
        }
    }
}