package tsfat.yeshivathahesder.channel.fragment


import tsfat.yeshivathahesder.channel.R
import tsfat.yeshivathahesder.channel.activity.VideoPlayerActivity
import tsfat.yeshivathahesder.channel.fastadapteritems.ProgressIndicatorItem
import tsfat.yeshivathahesder.channel.fastadapteritems.SearchItem
import tsfat.yeshivathahesder.channel.model.SearchedList
import tsfat.yeshivathahesder.channel.paging.Status
import tsfat.yeshivathahesder.channel.utils.DividerItemDecorator
import tsfat.yeshivathahesder.channel.viewmodel.SearchViewModel
import tsfat.yeshivathahesder.core.extensions.dismissKeyboard
import tsfat.yeshivathahesder.core.extensions.makeGone
import tsfat.yeshivathahesder.core.extensions.makeVisible
import tsfat.yeshivathahesder.core.extensions.showKeyboard
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
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
import kotlinx.android.synthetic.main.fragment_search.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import tsfat.yeshivathahesder.channel.di.AudioConnector
import tsfat.yeshivathahesder.channel.di.PlayVideo

@ExperimentalPagedSupport
class SearchFragment : Fragment() {

    private val viewModel by viewModel<SearchViewModel>() // Lazy inject ViewModel

    private lateinit var searchAdapter: GenericFastAdapter
    private lateinit var searchPagedModelAdapter: PagedModelAdapter<SearchedList.SearchItem, SearchItem>
    private lateinit var footerAdapter: GenericItemAdapter
    private var isFirstPageLoading = true
    private var retrySnackbar: Snackbar? = null
    private var isSearchRequestInitialized = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()

        setupSearchView()
        setupRecyclerView(savedInstanceState)
    }

    override fun onSaveInstanceState(_outState: Bundle) {
        var outState = _outState
        outState = searchAdapter.saveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        super.onPause()
        svSearch.dismissKeyboard(context)
        retrySnackbar?.dismiss() // Dismiss the retrySnackbar if already present
    }

    private fun setupToolbar() {
        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        toolbarSearch.setupWithNavController(navController, appBarConfiguration)
    }

    private fun setupSearchView() {
        svSearch.apply {
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    svSearch.dismissKeyboard(context)

                    if (query.isNotBlank()) {
                        pbSearch.makeVisible()

                        if (!isSearchRequestInitialized) {
                            isSearchRequestInitialized = true
                            viewModel.searchVideos(query)
                            setupObservables() // A bug arises and create() of DataSourceFactory is not called if observables are set before making an initial call :|
                        } else {
                            viewModel.setSearchQuery(query)
                        }
                    }
                    return true
                }

                override fun onQueryTextChange(p0: String?): Boolean {
                    return true
                }
            })

            // Set focus on the SearchView and open the keyboard
            setOnQueryTextFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    svSearch.findFocus().showKeyboard(context)
                }
            }
            requestFocus()
        }
    }

    private fun setupRecyclerView(savedInstanceState: Bundle?) {
        val asyncDifferConfig = AsyncDifferConfig.Builder<SearchedList.SearchItem>(object :
            DiffUtil.ItemCallback<SearchedList.SearchItem>() {
            override fun areItemsTheSame(
                oldItem: SearchedList.SearchItem,
                newItem: SearchedList.SearchItem
            ): Boolean {
                return oldItem.baseId == newItem.baseId
            }

            override fun areContentsTheSame(
                oldItem: SearchedList.SearchItem,
                newItem: SearchedList.SearchItem
            ): Boolean {
                return oldItem == newItem
            }
        }).build()

        searchPagedModelAdapter =
            PagedModelAdapter<SearchedList.SearchItem, SearchItem>(asyncDifferConfig) {
                SearchItem(it)
            }

        footerAdapter = ItemAdapter.items()

        searchAdapter = FastAdapter.with(listOf(searchPagedModelAdapter, footerAdapter))
        searchAdapter.registerTypeInstance(SearchItem(null))
        searchAdapter.withSavedInstanceState(savedInstanceState)

        rvSearch.layoutManager = LinearLayoutManager(context)
        rvSearch.addItemDecoration(
            DividerItemDecorator(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.view_divider_item_decorator
                )!!
            )
        )
        rvSearch.adapter = searchAdapter

        onItemClick()
    }

    private fun setupObservables() {
        // Observe Empty State LiveData
        viewModel.emptyStateLiveData.observe(viewLifecycleOwner, Observer { isResultEmpty ->
            if (isResultEmpty) {
                // True as no pages are loaded. If not done two loaders are shown when searched again.
                isFirstPageLoading = true
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
                    pbSearch.makeGone()
                    createRetrySnackbar()
                    retrySnackbar?.show()
                }

                Status.SUCCESS -> {
                    footerAdapter.clear()
                    pbSearch.makeGone()
                    hideEmptyState()
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
        viewModel.searchResultLiveData?.observe(
            viewLifecycleOwner,
            Observer<PagedList<SearchedList.SearchItem>> { videoList ->
                searchPagedModelAdapter.submitList(videoList)
            })
    }

    private fun showRecyclerViewProgressIndicator() {
        footerAdapter.clear()
        val progressIndicatorItem = ProgressIndicatorItem()
        footerAdapter.add(progressIndicatorItem)
    }

    private fun createRetrySnackbar() {
        retrySnackbar =
            Snackbar.make(clSearch, R.string.error_load_more_videos, Snackbar.LENGTH_INDEFINITE)
                .setAnchorView(activity?.findViewById(R.id.bottomNavView) as BottomNavigationView)
                .setAction(R.string.btn_retry) {
                    viewModel.refreshFailedRequest()
                }
    }

    private fun showEmptyState() {
        groupEmptySearch.makeVisible()
    }

    private fun hideEmptyState() {
        groupEmptySearch.makeGone()
    }

    private val audioConnector: AudioConnector by inject()
    private val playVideo: PlayVideo by inject()

    /**
     * Called when an item of the RecyclerView is clicked
     */
    private fun onItemClick() {
        searchAdapter.onClickListener = { view, adapter, item, position ->
            if (item is SearchItem) {
                if (item.searchedList is SearchedList.Item)
                    playVideo.play(context, item.searchedList.id.videoId)
                else if (item.searchedList is SearchedList.AudioSearchItem)
                    audioConnector.playItem(item.searchedList.item.mediaId)
            }
            false
        }
    }
}
