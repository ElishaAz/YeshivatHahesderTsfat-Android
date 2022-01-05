package tsfat.yeshivathahesder.channel.fragment


import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import android.view.View
import android.view.ViewGroup

import tsfat.yeshivathahesder.channel.R
import tsfat.yeshivathahesder.channel.fastadapteritems.FavoriteItem
import tsfat.yeshivathahesder.channel.viewmodel.FavoritesViewModel
import tsfat.yeshivathahesder.core.extensions.openUrl
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.listeners.ClickEventHook
import com.mikepenz.itemanimators.AlphaInAnimator
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import tsfat.yeshivathahesder.channel.databinding.FragmentCommentsBinding
import tsfat.yeshivathahesder.channel.databinding.FragmentFavoritesBinding
import tsfat.yeshivathahesder.channel.di.AudioConnector
import tsfat.yeshivathahesder.channel.di.PlayVideo
import tsfat.yeshivathahesder.channel.paging.datasource.PLAYLIST_TYPE_AUDIO
import tsfat.yeshivathahesder.channel.paging.datasource.PLAYLIST_TYPE_VIDEO

/**
 * A simple [Fragment] subclass.
 */
class FavoritesFragment : Fragment() {

    private val viewModel by viewModel<FavoritesViewModel>() // Lazy inject ViewModel

    private lateinit var favoritesAdapter: FastItemAdapter<FavoriteItem>

    private lateinit var binding: FragmentFavoritesBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()

        setupRecyclerView(savedInstanceState)
        setupObservables()
    }

    override fun onSaveInstanceState(_outState: Bundle) {
        var outState = _outState
        outState = favoritesAdapter.saveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    private fun setupToolbar() {
        binding.ablFavorites.toolbarMain.apply {
            inflateMenu(R.menu.main_menu)

            // Store and Search configuration
            menu.findItem(R.id.miStoreMainMenu).isVisible =
                resources.getBoolean(R.bool.enable_store)
            menu.findItem(R.id.miSearchMainMenu).isVisible =
                resources.getBoolean(R.bool.enable_search)

            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.miStoreMainMenu -> {
                        context.openUrl(getString(R.string.store_url), R.color.defaultBgColor)
                    }
                    R.id.miSearchMainMenu -> {
                        findNavController().navigate(R.id.action_favoritesFragment_to_searchFragment)
                    }
                }
                false
            }
        }
    }

    private fun setupRecyclerView(savedInstanceState: Bundle?) {
        favoritesAdapter = FastItemAdapter()
        favoritesAdapter.setHasStableIds(true)
        favoritesAdapter.withSavedInstanceState(savedInstanceState)

        binding.rvFavorites.layoutManager = LinearLayoutManager(context)
        binding.rvFavorites.itemAnimator = AlphaInAnimator()
        binding.rvFavorites.adapter = favoritesAdapter
        binding.rvFavorites.itemAnimator = AlphaInAnimator()

        onFavoriteClick()
        onItemClick()
    }

    private fun setupObservables() {
        viewModel.favoriteVideosLiveData.observe(viewLifecycleOwner, Observer { favoriteVideoList ->
            val favoriteItemsList = ArrayList<FavoriteItem>()
            for (favoriteVideo in favoriteVideoList) {
                favoriteItemsList.add(FavoriteItem(favoriteVideo))
            }

            favoritesAdapter.add(favoriteItemsList)
            showEmptyState(favoritesAdapter.itemCount)
        })
    }

    private fun showEmptyState(itemCount: Int) {
        binding.groupEmptyFavorites.isVisible = itemCount < 1
    }

    /**
     * Called when the Heart Icon is clicked of a RecyclerView Item
     */
    private fun onFavoriteClick() {
        favoritesAdapter.addEventHook(object : ClickEventHook<FavoriteItem>() {
            override fun onBind(viewHolder: RecyclerView.ViewHolder): View? {
                return if (viewHolder is FavoriteItem.FavoriteViewHolder) {
                    viewHolder.favoriteIcon
                } else {
                    null
                }
            }

            override fun onClick(
                v: View,
                position: Int,
                fastAdapter: FastAdapter<FavoriteItem>,
                item: FavoriteItem
            ) {
                val favoriteIcon = v as AppCompatImageView

                if (item.favoritesEntry.isChecked) {
                    // Icon unchecked
                    item.favoritesEntry.isChecked = false
                    favoriteIcon.setImageDrawable(
                        ContextCompat.getDrawable(
                            context!!,
                            R.drawable.ic_favorite_border
                        )
                    )
                    viewModel.removeVideoFromFavorites(item.favoritesEntry)
                } else {
                    // Icon checked
                    item.favoritesEntry.isChecked = true
                    favoriteIcon.setImageDrawable(
                        ContextCompat.getDrawable(
                            context!!,
                            R.drawable.ic_favorite_filled_border
                        )
                    )
                    viewModel.addVideoToFavorites(item.favoritesEntry)
                }
            }
        })
    }


    private val playVideo: PlayVideo by inject()
    private val audioConnector: AudioConnector by inject()

    /**
     * Called when an item of the RecyclerView is clicked
     */
    private fun onItemClick() {
        favoritesAdapter.onClickListener = { view, adapter, item, position ->
            if (item.favoritesEntry.type == PLAYLIST_TYPE_VIDEO) {
                playVideo.play(context, item.favoritesEntry.id)
            } else if (item.favoritesEntry.type == PLAYLIST_TYPE_AUDIO) {
                audioConnector.playItem(item.favoritesEntry.id)
            }
            false
        }
    }
}
