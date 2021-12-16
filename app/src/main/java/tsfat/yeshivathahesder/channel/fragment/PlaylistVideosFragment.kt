package tsfat.yeshivathahesder.channel.fragment


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import tsfat.yeshivathahesder.channel.R
import tsfat.yeshivathahesder.channel.activity.VideoPlayerActivity
import tsfat.yeshivathahesder.channel.fastadapteritems.PlaylistVideoItem
import tsfat.yeshivathahesder.channel.fastadapteritems.ProgressIndicatorItem
import tsfat.yeshivathahesder.channel.paging.Status
import tsfat.yeshivathahesder.channel.utils.DateTimeUtils
import tsfat.yeshivathahesder.channel.utils.DividerItemDecorator
import tsfat.yeshivathahesder.channel.viewmodel.PlaylistVideosViewModel
import tsfat.yeshivathahesder.core.extensions.startShareTextIntent
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.paging.PagedList
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import coil.api.load
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericFastAdapter
import com.mikepenz.fastadapter.adapters.GenericItemAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.paged.PagedModelAdapter
import kotlinx.android.synthetic.main.fragment_playlist_details.view.*
import kotlinx.android.synthetic.main.fragment_playlist_videos.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import tsfat.yeshivathahesder.channel.model.ItemBase
import tsfat.yeshivathahesder.channel.model.VideoItem

/**
 * A simple [Fragment] subclass to show the list of videos of a particular playlist.
 */
class PlaylistVideosFragment : Fragment() {

    private val viewModel by viewModel<PlaylistVideosViewModel>() // Lazy inject ViewModel
    private val args by navArgs<PlaylistVideosFragmentArgs>()

    private lateinit var playlistVideosAdapter: GenericFastAdapter
    private lateinit var playlistVideosPagedModelAdapter: PagedModelAdapter<ItemBase, PlaylistVideoItem>
    private lateinit var footerAdapter: GenericItemAdapter
    private var isFirstPageLoading = true
    private var retrySnackbar: Snackbar? = null
    private lateinit var playlistId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_playlist_videos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        playlistId = args.playlistId
        setupToolbar()

        setupRecyclerView(savedInstanceState)
        fetchPlaylistVideos()
        setupObservables()
    }

    override fun onSaveInstanceState(_outState: Bundle) {
        var outState = _outState
        outState = playlistVideosAdapter.saveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        super.onPause()
        retrySnackbar?.dismiss() // Dismiss the retrySnackbar if already present
    }

    private fun setupToolbar() {
        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        toolbarPlaylistVideos.setupWithNavController(navController, appBarConfiguration)

        // Inflate Menu
        toolbarPlaylistVideos.inflateMenu(R.menu.toolbar_menu_playlist_videos)
        toolbarPlaylistVideos.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.miDetailsPlaylistVideos -> {
                    showPlaylistDetails()
                }
                R.id.miSharePlaylistVideos -> {
                    sharePlaylistUrl()
                }
            }
            true
        }
    }

    private fun setupRecyclerView(savedInstanceState: Bundle?) {
        val asyncDifferConfig = AsyncDifferConfig.Builder<ItemBase>(object :
            DiffUtil.ItemCallback<ItemBase>() {
            override fun areItemsTheSame(
                oldItem: ItemBase,
                newItem: ItemBase
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: ItemBase,
                newItem: ItemBase
            ): Boolean {
                return oldItem == newItem
            }
        }).build()

        playlistVideosPagedModelAdapter =
            PagedModelAdapter<ItemBase, PlaylistVideoItem>(asyncDifferConfig) {
                PlaylistVideoItem(it)
            }

        footerAdapter = ItemAdapter.items()

        playlistVideosAdapter =
            FastAdapter.with(listOf(playlistVideosPagedModelAdapter, footerAdapter))
        playlistVideosAdapter.registerTypeInstance(PlaylistVideoItem(null))
        playlistVideosAdapter.withSavedInstanceState(savedInstanceState)

        rvPlaylistVideos.layoutManager = LinearLayoutManager(context)
        rvPlaylistVideos.adapter = playlistVideosAdapter
        rvPlaylistVideos.addItemDecoration(
            DividerItemDecorator(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.view_divider_item_decorator
                )!!
            )
        )

        onItemClick()
    }

    private fun fetchPlaylistVideos() {
        viewModel.getPlaylistVideos(playlistId)
    }

    private fun setupObservables() {
        // Observe network live data
        viewModel.networkStateLiveData?.observe(viewLifecycleOwner, Observer { networkState ->
            when (networkState?.status) {
                Status.FAILED -> {
                    footerAdapter.clear()
                    createRetrySnackbar()
                    retrySnackbar?.show()
                }
                Status.SUCCESS -> {
                    footerAdapter.clear()
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
        viewModel.playlistVideosLiveData?.observe(
            viewLifecycleOwner,
            Observer<PagedList<ItemBase>> { playlistVideosList ->
                playlistVideosPagedModelAdapter.submitList(playlistVideosList)
            })
    }

    private fun showRecyclerViewProgressIndicator() {
        footerAdapter.clear()
        val progressIndicatorItem = ProgressIndicatorItem()
        footerAdapter.add(progressIndicatorItem)
    }

    private fun createRetrySnackbar() {
        retrySnackbar =
            Snackbar.make(
                clPlaylistVideos,
                R.string.error_load_more_videos,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAnchorView(activity?.findViewById(R.id.bottomNavView) as BottomNavigationView)
                .setAction(R.string.btn_retry) {
                    viewModel.refreshFailedRequest()
                }
    }

    /**
     * Called when an item of the RecyclerView is clicked
     */
    private fun onItemClick() {
        playlistVideosAdapter.onClickListener = { view, adapter, item, position ->
            if (item is PlaylistVideoItem) {
                val playlistItem: ItemBase = item.playlistItem!!
                if (playlistItem is VideoItem) {
                    VideoPlayerActivity.startActivity(
                        context,
                        playlistItem.contentDetails.videoId
                    )
                }
            }
            false
        }
    }

    private fun showPlaylistDetails() {
        val playlistDetailsDialog =
            MaterialDialog(requireContext(), BottomSheet(LayoutMode.WRAP_CONTENT)).show {
                customView(R.layout.fragment_playlist_details, scrollable = true)

            }

        playlistDetailsDialog.getCustomView().apply {
            ivThumbnailPlaylistDetails.load(args.playlistThumbUrl)
            tvNamePlaylistDetails.text = args.playlistName
            tvVideoCountPlaylistDetails.text = context.resources.getQuantityString(
                R.plurals.text_playlist_video_count,
                args.playlistVideoCount.toInt(),
                args.playlistVideoCount.toInt()
            )
            tvTimePublishedPlaylistDetails.text = context.getString(
                R.string.text_playlist_published_date,
                DateTimeUtils.getPublishedDate(args.playlistPublishedTime)
            )
            tvDescPlaylistDetails.text = args.playlistDesc
        }
    }

    private fun sharePlaylistUrl() {
        context?.startShareTextIntent(
            getString(R.string.text_share_playlist),
            getString(R.string.text_playlist_share_url, args.playlistId)
        )
    }
}
