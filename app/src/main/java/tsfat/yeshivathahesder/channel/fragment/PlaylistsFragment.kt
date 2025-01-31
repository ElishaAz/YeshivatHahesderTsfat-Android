package tsfat.yeshivathahesder.channel.fragment


import tsfat.yeshivathahesder.channel.R
import tsfat.yeshivathahesder.channel.fastadapteritems.PlaylistItem
import tsfat.yeshivathahesder.channel.fastadapteritems.ProgressIndicatorItem
import tsfat.yeshivathahesder.channel.model.Playlists
import tsfat.yeshivathahesder.channel.paging.Status
import tsfat.yeshivathahesder.channel.utils.DividerItemDecorator
import tsfat.yeshivathahesder.channel.viewmodel.PlaylistsViewModel
import tsfat.yeshivathahesder.core.extensions.makeGone
import tsfat.yeshivathahesder.core.extensions.makeVisible
import tsfat.yeshivathahesder.core.extensions.openUrl
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.paging.PagedList
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericFastAdapter
import com.mikepenz.fastadapter.adapters.GenericItemAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.paged.ExperimentalPagedSupport
import com.mikepenz.fastadapter.paged.PagedModelAdapter
import org.koin.androidx.viewmodel.ext.android.viewModel
import tsfat.yeshivathahesder.channel.databinding.FragmentPlaylistsBinding
import tsfat.yeshivathahesder.channel.model.PlaylistBase
import tsfat.yeshivathahesder.channel.paging.datasource.PLAYLIST_TYPE_AUDIO
import tsfat.yeshivathahesder.channel.paging.datasource.PLAYLIST_TYPE_VIDEO
import tsfat.yeshivathahesder.channel.uamp.AudioPlaylist
import tsfat.yeshivathahesder.channel.uamp.media.library.defaultAudioUri

/**
 * A simple [Fragment] subclass to show the list of
 * Playlists of a channel.
 */
@ExperimentalPagedSupport
class PlaylistsFragment : Fragment() {

    private val viewModel by viewModel<PlaylistsViewModel>() // Lazy inject ViewModel

    private lateinit var playlistsAdapter: GenericFastAdapter
    private lateinit var playlistsPagedModelAdapter: PagedModelAdapter<PlaylistBase, PlaylistItem>
    private lateinit var footerAdapter: GenericItemAdapter
    private var isFirstPageLoading = true
    private var retrySnackbar: Snackbar? = null

    private lateinit var binding: FragmentPlaylistsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlaylistsBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()

        setupRecyclerView(savedInstanceState)
        fetchPlaylists()
        setupObservables()
    }

    override fun onSaveInstanceState(_outState: Bundle) {
        var outState = _outState
        outState = playlistsAdapter.saveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        super.onPause()
        retrySnackbar?.dismiss() // Dismiss the retrySnackbar if already present
    }

    private fun setupToolbar() {
        binding.ablPlaylists.toolbarMain.apply {
            inflateMenu(R.menu.main_menu)

            // Store and Search configuration
            menu.findItem(R.id.miStoreMainMenu).isVisible =
                resources.getBoolean(R.bool.enable_store)
            menu.findItem(R.id.miSearchMainMenu).isVisible =
                resources.getBoolean(R.bool.enable_search)

            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.miStoreMainMenu -> {
                        context.openUrl(getString(R.string.store_url))
                    }
                    R.id.miSearchMainMenu -> {
                        findNavController().navigate(R.id.action_playlistsFragment_to_searchFragment)
                    }
                }
                false
            }
        }
    }

    private fun setupRecyclerView(savedInstanceState: Bundle?) {
        val asyncDifferConfig = AsyncDifferConfig.Builder<PlaylistBase>(object :
            DiffUtil.ItemCallback<PlaylistBase>() {
            override fun areItemsTheSame(
                oldItem: PlaylistBase,
                newItem: PlaylistBase
            ): Boolean {
                return oldItem.baseId == newItem.baseId
            }

            override fun areContentsTheSame(
                oldItem: PlaylistBase,
                newItem: PlaylistBase
            ): Boolean {
                return oldItem == newItem
            }
        }).build()

        playlistsPagedModelAdapter =
            PagedModelAdapter<PlaylistBase, PlaylistItem>(asyncDifferConfig) {
                PlaylistItem(it)
            }

        footerAdapter = ItemAdapter.items()

        playlistsAdapter = FastAdapter.with(listOf(playlistsPagedModelAdapter, footerAdapter))
        playlistsAdapter.registerTypeInstance(PlaylistItem(null))
        playlistsAdapter.withSavedInstanceState(savedInstanceState)

        binding.rvPlaylists.layoutManager = LinearLayoutManager(context)
        binding.rvPlaylists.addItemDecoration(
            DividerItemDecorator(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.view_divider_item_decorator
                )!!
            )
        )
        binding.rvPlaylists.adapter = playlistsAdapter
        onItemClick()
    }

    private fun setupObservables() {
        // Observe Empty State LiveData
        viewModel.emptyStateLiveData.observe(viewLifecycleOwner, Observer { isResultEmpty ->
            if (isResultEmpty) {
                showEmptyState()
            } else {
                hideEmptyState()
            }
        })

        // Observe network live data
        viewModel.networkStateLiveData?.observe(viewLifecycleOwner, Observer { networkState ->
            when (networkState?.status) {
                Status.FAILED -> {
                    footerAdapter.clear()
                    binding.pbPlaylists.makeGone()
                    createRetrySnackbar()
                    retrySnackbar?.show()
                }
                Status.SUCCESS -> {
                    footerAdapter.clear()
                    binding.pbPlaylists.makeGone()
                }
                Status.LOADING -> {
                    if (!isFirstPageLoading) {
                        showRecyclerViewProgressIndicator()
                    } else {
                        isFirstPageLoading = false
                    }
                }
            }
        })

        // Observe latest video live data
        viewModel.playlistsLiveData?.observe(
            viewLifecycleOwner,
            Observer<PagedList<PlaylistBase>> { playlistsList ->
                playlistsPagedModelAdapter.submitList(playlistsList)
            })
    }

    private fun showRecyclerViewProgressIndicator() {
        footerAdapter.clear()
        val progressIndicatorItem = ProgressIndicatorItem()
        footerAdapter.add(progressIndicatorItem)
    }

    private fun showEmptyState() {
        binding.groupEmptyPlaylists.makeVisible()
    }

    private fun hideEmptyState() {
        binding.groupEmptyPlaylists.makeGone()
    }

    private fun fetchPlaylists() {
        viewModel.getPlaylists()
    }

    private fun createRetrySnackbar() {
        retrySnackbar =
            Snackbar.make(
                binding.clPlaylists,
                R.string.error_fetch_playlists,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAnchorView(activity?.findViewById(R.id.bottomNavView) as BottomNavigationView)
                .setAction(R.string.btn_retry) {
                    viewModel.refreshFailedRequest()
                }
    }

    private fun onItemClick() {
        playlistsAdapter.onClickListener = { view, adapter, item, position ->
            if (item is PlaylistItem) {
                val action = item.playlistItem.let {
                    if (it is Playlists.VideoPlaylist) {
                        PlaylistsFragmentDirections.actionPlaylistsFragmentToPlaylistVideosFragment(
                            it.snippet.title,
                            PLAYLIST_TYPE_VIDEO,
                            it.id,
                            it.snippet.description,
                            it.contentDetails.itemCount.toFloat(),
                            it.snippet.thumbnails.resUrl,
                            it.snippet.publishedAt
                        )
                    } else if (it is AudioPlaylist) {
                        PlaylistsFragmentDirections.actionPlaylistsFragmentToPlaylistVideosFragment(
                            it.name,
                            PLAYLIST_TYPE_AUDIO,
                            it.mediaId,
                            it.name,
                            it.itemCount.toFloat(),
                            defaultAudioUri,
                            it.publishedAt
                        )
                    } else {
                        null
                    }
                }

                findNavController().navigate(action!!)
            }
            false
        }
    }
}
