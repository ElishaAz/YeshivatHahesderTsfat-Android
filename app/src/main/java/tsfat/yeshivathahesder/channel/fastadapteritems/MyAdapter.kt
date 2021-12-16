package tsfat.yeshivathahesder.channel.fastadapteritems

import com.mikepenz.fastadapter.AbstractAdapter
import com.mikepenz.fastadapter.paged.PagedModelAdapter
import tsfat.yeshivathahesder.channel.model.ItemBase

class MyAdapter(private val pagedAdapter: PagedModelAdapter<ItemBase,HomeItem>): AbstractAdapter<HomeItem>() {
    override val adapterItemCount: Int
        get() = TODO("Not yet implemented")
    override val adapterItems: List<HomeItem>
        get() = TODO("Not yet implemented")

    override fun getAdapterItem(position: Int): HomeItem {
        TODO("Not yet implemented")
    }

    override fun getAdapterPosition(item: HomeItem): Int {
        TODO("Not yet implemented")
    }

    override fun getAdapterPosition(identifier: Long): Int {
        TODO("Not yet implemented")
    }

    override fun getGlobalPosition(position: Int): Int {
        TODO("Not yet implemented")
    }
}