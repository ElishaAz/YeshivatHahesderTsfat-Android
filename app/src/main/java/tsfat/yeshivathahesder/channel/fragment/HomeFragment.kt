package tsfat.yeshivathahesder.channel.fragment

import tsfat.yeshivathahesder.channel.R
import tsfat.yeshivathahesder.channel.activity.VideoPlayerActivity
import tsfat.yeshivathahesder.channel.fastadapteritems.HomeItem
import tsfat.yeshivathahesder.channel.fastadapteritems.ProgressIndicatorItem
import tsfat.yeshivathahesder.channel.paging.Status
import tsfat.yeshivathahesder.channel.utils.DividerItemDecorator
import tsfat.yeshivathahesder.channel.viewmodel.HomeViewModel
import tsfat.yeshivathahesder.core.extensions.*
import tsfat.yeshivathahesder.core.helper.ResultWrapper
import android.os.Bundle
import android.view.View
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
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.fastadapter.paged.ExperimentalPagedSupport
import com.mikepenz.fastadapter.paged.PagedModelAdapter
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.widget_toolbar.view.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import tsfat.yeshivathahesder.channel.di.AudioConnector
import tsfat.yeshivathahesder.channel.model.ItemBase
import tsfat.yeshivathahesder.channel.model.VideoItem
import tsfat.yeshivathahesder.channel.uamp.AudioItem

@ExperimentalPagedSupport
class HomeFragment : Fragment(R.layout.fragment_home) {

    private val viewModel by viewModel<HomeViewModel>() // Lazy inject ViewModel

    private var homeAdapter: GenericFastAdapter? = null
    private lateinit var homePagedModelAdapter: PagedModelAdapter<ItemBase, HomeItem>
    private lateinit var footerAdapter: GenericItemAdapter
    private var isFirstPageLoading = true
    private var retrySnackbar: Snackbar? = null


    private val audioConnector: AudioConnector by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()

        if (requireContext().isInternetAvailable()) {
            viewModel.getLatestVideos()
        } else {
            showErrorState()
        }

        setupUploadsPlaylistIdObservables()
        setupRecyclerView(savedInstanceState)
        onRetryButtonClick()

        audioConnector.audioItems.observe(viewLifecycleOwner) {
        }
    }

    override fun onSaveInstanceState(_outState: Bundle) {
        homeAdapter?.let {
            var outState = _outState
            outState = it.saveInstanceState(outState)
            super.onSaveInstanceState(outState)
        }

    }

    override fun onPause() {
        super.onPause()
        retrySnackbar?.dismiss() // Dismiss the retrySnackbar if already present
    }

    private fun setupToolbar() {
        ablHome.toolbarMain.apply {
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
                        findNavController().navigate(R.id.action_homeFragment_to_searchFragment)
                    }
                }
                false
            }
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

        homePagedModelAdapter =
            PagedModelAdapter<ItemBase, HomeItem>(asyncDifferConfig) {
                HomeItem(it)
            }

        footerAdapter = ItemAdapter.items()

        homeAdapter = FastAdapter()

        homeAdapter = FastAdapter.with(listOf(homePagedModelAdapter, footerAdapter))
        homeAdapter?.registerTypeInstance(HomeItem(null))
        homeAdapter?.withSavedInstanceState(savedInstanceState)

        rvHome.layoutManager = LinearLayoutManager(context)
        rvHome.adapter = homeAdapter
        rvHome.addItemDecoration(
            DividerItemDecorator(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.view_divider_item_decorator
                )!!
            )
        )
        onItemClick()
    }

    private fun setupUploadsPlaylistIdObservables() {
        viewModel.uploadsPlaylistIdLiveData.observe(viewLifecycleOwner, Observer {
            when (it) {
                is ResultWrapper.Loading -> {
                    // Data is always fetched from the next page and hence loadBefore is never needed
                }
                is ResultWrapper.Error -> {
                    // Error occurred while fetching the uploads playlist id
                    showErrorState(it.errorMessage)
                }
                is ResultWrapper.Success<*> -> {
                    // Success in fetching the uploads playlist id
                    hideErrorState()
                    setupLatestVideosObservables()
                }
            }
        })
    }

    private fun setupLatestVideosObservables() {
        // Observe network live data
        viewModel.networkStateLiveData?.observe(viewLifecycleOwner, Observer { networkState ->
            when (networkState?.status) {
                Status.FAILED -> {
                    footerAdapter.clear()
                    pbHome.makeGone()
                    createRetrySnackbar()
                    retrySnackbar?.show()
                }
                Status.SUCCESS -> {
                    footerAdapter.clear()
                    pbHome.makeGone()
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
        viewModel.latestVideoLiveData?.observe(
            viewLifecycleOwner,
            Observer<PagedList<ItemBase>> { latestVideoList ->
                homePagedModelAdapter.submitList(latestVideoList)
            })
    }

    private fun showRecyclerViewProgressIndicator() {
        footerAdapter.clear()
        val progressIndicatorItem = ProgressIndicatorItem()
        footerAdapter.add(progressIndicatorItem)
    }

    private fun showErrorState(errorMsg: String = getString(R.string.error_internet_connectivity)) {
        rvHome.makeGone()
        pbHome.makeGone()
        groupErrorHome.makeVisible()
        tvErrorHome.text = errorMsg
    }

    private fun hideErrorState() {
        groupErrorHome.makeGone()
        rvHome.makeVisible()
    }

    /**
     * Called when the Retry button of the error state is clicked
     */
    private fun onRetryButtonClick() {
        btnRetryHome.setOnClickListener {
            if (requireContext().isInternetAvailable()) viewModel.getLatestVideos()
        }
    }

    /**
     * Called when an item of the RecyclerView is clicked
     */
    private fun onItemClick() {
        homeAdapter?.onClickListener = { view, adapter, item, position ->
            if (item is HomeItem) {
                val mediaItem: ItemBase = item.playlistItem!!
                if (mediaItem is VideoItem) {
                    VideoPlayerActivity.startActivity(
                        context,
                        mediaItem.contentDetails.videoId
                    )
                } else if (mediaItem is AudioItem) {
                    audioConnector.playItem(mediaItem)
//                    AudioPlayerActivity.startActivity(context, mediaItem.contentDetails.audioId)
                }
            }

            false
        }
    }

    private fun createRetrySnackbar() {
        retrySnackbar =
            Snackbar.make(clHome, R.string.error_load_more_videos, Snackbar.LENGTH_INDEFINITE)
                .setAnchorView(activity?.findViewById(R.id.bottomNavView) as BottomNavigationView)
                .setAction(R.string.btn_retry) {
                    viewModel.refreshFailedRequest()
                }
    }
}
