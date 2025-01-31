package tsfat.yeshivathahesder.channel.fragment

import tsfat.yeshivathahesder.channel.R
import tsfat.yeshivathahesder.channel.fastadapteritems.CommentItem
import tsfat.yeshivathahesder.channel.fastadapteritems.ProgressIndicatorItem
import tsfat.yeshivathahesder.channel.model.Comment
import tsfat.yeshivathahesder.channel.paging.Status
import tsfat.yeshivathahesder.channel.utils.DividerItemDecorator
import tsfat.yeshivathahesder.channel.viewmodel.CommentsViewModel
import tsfat.yeshivathahesder.core.extensions.makeGone
import tsfat.yeshivathahesder.core.extensions.makeVisible
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.PagedList
import androidx.recyclerview.widget.*
import com.google.android.material.snackbar.Snackbar
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericFastAdapter
import com.mikepenz.fastadapter.adapters.GenericItemAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.listeners.ClickEventHook
import com.mikepenz.fastadapter.paged.PagedModelAdapter
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import tsfat.yeshivathahesder.channel.databinding.FragmentCommentsBinding

class CommentsFragment : Fragment() {

    private val viewModel by viewModel<CommentsViewModel>() // Lazy inject ViewModel
    private val args by navArgs<CommentsFragmentArgs>()

    private lateinit var videoId: String
    private lateinit var commentsAdapter: GenericFastAdapter
    private lateinit var commentsPagedModelAdapter: PagedModelAdapter<Comment.Item, CommentItem>
    private lateinit var footerAdapter: GenericItemAdapter
    private var isFirstPageLoading = true
    private var retrySnackbar: Snackbar? = null
    private val SORT_BY_RELEVANCE = "relevance"
    private val SORT_BY_TIME = "time"

    private var initialLayoutComplete = false

    private lateinit var binding: FragmentCommentsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCommentsBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        videoId = args.videoId

        setupRecyclerView(savedInstanceState)
        fetchComments(SORT_BY_RELEVANCE)
        setupObservables()

        binding.ivSortComments.setOnClickListener { onSortClick(it) }
        binding.ivCloseComments.setOnClickListener { onCloseClick() }
    }

    override fun onSaveInstanceState(_outState: Bundle) {
        var outState = _outState
        outState = commentsAdapter.saveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        retrySnackbar?.dismiss() // Dismiss the retrySnackbar if already present
        super.onPause()
    }

    private fun setupRecyclerView(savedInstanceState: Bundle?) {
        val asyncDifferConfig = AsyncDifferConfig.Builder<Comment.Item>(object :
            DiffUtil.ItemCallback<Comment.Item>() {
            override fun areItemsTheSame(
                oldItem: Comment.Item,
                newItem: Comment.Item
            ): Boolean {
                return oldItem.snippet.topLevelComment.id == newItem.snippet.topLevelComment.id
            }

            override fun areContentsTheSame(
                oldItem: Comment.Item,
                newItem: Comment.Item
            ): Boolean {
                return oldItem == newItem
            }
        }).build()

        commentsPagedModelAdapter =
            PagedModelAdapter<Comment.Item, CommentItem>(asyncDifferConfig) {
                CommentItem(it)
            }

        footerAdapter = ItemAdapter.items()

        commentsAdapter = FastAdapter.with(listOf(commentsPagedModelAdapter, footerAdapter))
        commentsAdapter.registerTypeInstance(CommentItem(null))
        commentsAdapter.withSavedInstanceState(savedInstanceState)
        onViewRepliesClick()

        binding.rvComments.layoutManager = LinearLayoutManager(context)
        binding.rvComments.addItemDecoration(
            DividerItemDecorator(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.view_divider_item_decorator
                )!!
            )
        )
        binding.rvComments.adapter = commentsAdapter
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
        viewModel.commentsLiveData?.observe(
            viewLifecycleOwner,
            Observer<PagedList<Comment.Item>> { commentsList ->
                commentsPagedModelAdapter.submitList(commentsList)
            })
    }

    private fun showRecyclerViewProgressIndicator() {
        footerAdapter.clear()
        val progressIndicatorItem = ProgressIndicatorItem()
        footerAdapter.add(progressIndicatorItem)
    }

    private fun showEmptyState() {
        binding.groupEmptyComments.makeVisible()
    }

    private fun hideEmptyState() {
        binding.groupEmptyComments.makeGone()
    }

    private fun fetchComments(sortOrder: String) {
        viewModel.getVideoComments(videoId, sortOrder)
    }

    private fun createRetrySnackbar() {
        retrySnackbar =
            Snackbar.make(
                binding.clComments,
                R.string.error_fetch_comments,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.btn_retry) {
                    viewModel.refreshFailedRequest()
                }
    }

    /**
     * Called when the Sort icon is clicked
     */
    private fun onSortClick(view: View) {
        val sortMenu = PopupMenu(requireContext(), view)
        sortMenu.menuInflater.inflate(R.menu.comments_sort_menu_video_details, sortMenu.menu)
        sortMenu.show()

        sortMenu.setOnMenuItemClickListener { item: MenuItem? ->
            when (item?.itemId) {
                R.id.miTopComments -> {
                    viewModel.sortComments(SORT_BY_RELEVANCE)
                }
                R.id.miNewestFirstComments -> {
                    viewModel.sortComments(SORT_BY_TIME)
                    Timber.e("Newest Comments")
                }
            }
            false
        }

    }

    private fun onCloseClick() {
        findNavController().popBackStack()
    }

    /**
     * Called when the View Replies button is clicked of a
     * Comment Item
     */
    private fun onViewRepliesClick() {
        commentsAdapter.addEventHook(object : ClickEventHook<CommentItem>() {
            override fun onBind(viewHolder: RecyclerView.ViewHolder): View? {
                return if (viewHolder is CommentItem.CommentViewHolder) {
                    viewHolder.btnViewRepliesCommentItem
                } else {
                    null
                }
            }

            override fun onClick(
                v: View,
                position: Int,
                fastAdapter: FastAdapter<CommentItem>,
                item: CommentItem
            ) {
                val action =
                    CommentsFragmentDirections.actionCommentsFragmentToCommentRepliesFragment(item.comment?.snippet?.topLevelComment?.id!!)
                findNavController().navigate(action)
            }
        })
    }
}

