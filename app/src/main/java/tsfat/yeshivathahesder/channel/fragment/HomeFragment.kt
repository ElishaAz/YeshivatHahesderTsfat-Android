package tsfat.yeshivathahesder.channel.fragment

import tsfat.yeshivathahesder.channel.R
import tsfat.yeshivathahesder.channel.fastadapteritems.HomeItem
import tsfat.yeshivathahesder.channel.fastadapteritems.ProgressIndicatorItem
import tsfat.yeshivathahesder.channel.paging.Status
import tsfat.yeshivathahesder.channel.utils.DividerItemDecorator
import tsfat.yeshivathahesder.channel.viewmodel.HomeViewModel
import tsfat.yeshivathahesder.core.extensions.*
import tsfat.yeshivathahesder.core.helper.ResultWrapper
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.map
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
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import tsfat.yeshivathahesder.channel.databinding.FragmentHomeBinding
import tsfat.yeshivathahesder.channel.di.AudioConnector
import tsfat.yeshivathahesder.channel.di.PlayVideo
import tsfat.yeshivathahesder.channel.fastadapteritems.LiveVideoItem
import tsfat.yeshivathahesder.channel.model.ItemBase
import tsfat.yeshivathahesder.channel.model.VideoItem
import tsfat.yeshivathahesder.channel.uamp.AudioItem

@ExperimentalPagedSupport
class HomeFragment : Fragment() {

    private val viewModel by viewModel<HomeViewModel>() // Lazy inject ViewModel

    private var homeAdapter: GenericFastAdapter? = null
    private lateinit var homePagedModelAdapter: PagedModelAdapter<ItemBase, HomeItem>
    private lateinit var footerAdapter: GenericItemAdapter
    private lateinit var liveAdapter: GenericItemAdapter
    private var isFirstPageLoading = true
    private var retrySnackbar: Snackbar? = null

    private val audioConnector: AudioConnector by inject()

    private lateinit var binding: FragmentHomeBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("Home Fragment View Created")
        setupToolbar()

        if (requireContext().isInternetAvailable()) {
            viewModel.getLatestVideos()
        } else {
            showErrorState()
        }

        setupUploadsPlaylistIdObservables()
        setUpLiveVideosObservables()
        setupRecyclerView(savedInstanceState)
        onRetryButtonClick()
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
        binding.ablHome.toolbarMain.apply {
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
                return oldItem.baseId == newItem.baseId
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
        liveAdapter = ItemAdapter.items()

        homeAdapter = FastAdapter()

        homeAdapter = FastAdapter.with(listOf(liveAdapter, homePagedModelAdapter, footerAdapter))
//        homeAdapter?.registerTypeInstance(HomeItem(null))
        HomeItem(null).let { homeAdapter?.registerItemFactory(it.type, it) }

        homeAdapter?.withSavedInstanceState(savedInstanceState)

        binding.rvHome.layoutManager = LinearLayoutManager(context)
        binding.rvHome.adapter = homeAdapter
        binding.rvHome.addItemDecoration(
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
                    binding.pbHome.makeGone()
                    createRetrySnackbar()
                    retrySnackbar?.show()
                }
                Status.SUCCESS -> {
                    footerAdapter.clear()
                    binding.pbHome.makeGone()
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
                Timber.d("Got home list")
                homePagedModelAdapter.submitList(latestVideoList)
            })
    }

    private fun setUpLiveVideosObservables() {
        viewModel.liveVideosLiveData.observe(viewLifecycleOwner) {
            liveAdapter.clear()
            viewModel.liveVideosLiveData.value?.let { liveAdapter.set(it.map { LiveVideoItem(it) }) }
        }
    }

    private fun showRecyclerViewProgressIndicator() {
        footerAdapter.clear()
        val progressIndicatorItem = ProgressIndicatorItem()
        footerAdapter.add(progressIndicatorItem)
    }

    private fun showErrorState(errorMsg: String = getString(R.string.error_internet_connectivity)) {
        binding.rvHome.makeGone()
        binding.pbHome.makeGone()
        binding.groupErrorHome.makeVisible()
        binding.tvErrorHome.text = errorMsg
    }

    private fun hideErrorState() {
        binding.groupErrorHome.makeGone()
        binding.rvHome.makeVisible()
    }

    /**
     * Called when the Retry button of the error state is clicked
     */
    private fun onRetryButtonClick() {
        binding.btnRetryHome.setOnClickListener {
            if (requireContext().isInternetAvailable()) viewModel.getLatestVideos()
        }
    }


    private val playVideo: PlayVideo by inject()

    /**
     * Called when an item of the RecyclerView is clicked
     */
    private fun onItemClick() {
        homeAdapter?.onClickListener = { view, adapter, item, position ->
            if (item is HomeItem) {
                val mediaItem: ItemBase = item.playlistItem!!
                if (mediaItem is VideoItem) {
                    playVideo.play(
                        context,
                        mediaItem.contentDetails.videoId
                    )
                } else if (mediaItem is AudioItem) {
                    audioConnector.playItem(mediaItem.mediaId)
//                    AudioPlayerActivity.startActivity(context, mediaItem.contentDetails.audioId)
                }
            }

            false
        }
    }

    private fun createRetrySnackbar() {
        retrySnackbar =
            Snackbar.make(
                binding.clHome,
                R.string.error_load_more_videos,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAnchorView(activity?.findViewById(R.id.bottomNavView) as BottomNavigationView)
                .setAction(R.string.btn_retry) {
                    viewModel.refreshFailedRequest()
                }
    }
}
