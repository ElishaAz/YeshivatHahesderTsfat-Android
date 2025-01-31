package tsfat.yeshivathahesder.channel.fragment


import tsfat.yeshivathahesder.channel.R
import tsfat.yeshivathahesder.channel.model.FavoritesEntry
import tsfat.yeshivathahesder.channel.model.Video
import tsfat.yeshivathahesder.channel.utils.DateTimeUtils
import tsfat.yeshivathahesder.channel.viewmodel.VideoDetailsViewModel
import tsfat.yeshivathahesder.core.extensions.*
import tsfat.yeshivathahesder.core.helper.ResultWrapper
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import org.koin.androidx.viewmodel.ext.android.viewModel
import tsfat.yeshivathahesder.channel.activity.VideoPlayerActivity
import tsfat.yeshivathahesder.channel.databinding.FragmentVideoDetailsBinding
import tsfat.yeshivathahesder.channel.paging.datasource.PLAYLIST_TYPE_VIDEO

class VideoDetailsFragment : Fragment() {

    companion object {
        const val VIDEO_ID = "videoId"
    }

    private val viewModel by viewModel<VideoDetailsViewModel>() // Lazy inject ViewModel
    private val args by navArgs<VideoDetailsFragmentArgs>()

    private lateinit var videoId: String
    private lateinit var videoItem: Video.Item
    private var isVideoAddedToFavorite = false

    private lateinit var binding: FragmentVideoDetailsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentVideoDetailsBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupObservables()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        videoId = args.videoId

        fetchVideoInfo()
        fetchVideoFavoriteStatus()

        setupBottomAppBar()
        binding.tvCommentsVideoDetails.setOnClickListener { onCommentsClick() }
        binding.btnRetryVideoDetails.setOnClickListener { onRetryClick() }
    }


    /**
     * Fetches the info of video
     */
    private fun fetchVideoInfo() {
        if (isInternetAvailable(requireContext())) {
            viewModel.getVideoInfo(videoId)
        } else {
            showVideoInfoErrorState()
        }
    }

    /**
     * Checks whether the current playing video is already added to favorites or not
     */
    private fun fetchVideoFavoriteStatus() {
        viewModel.getVideoFavoriteStatus(videoId)
    }

    private fun setupObservables() {
        viewModel.videoInfoLiveData.observe(viewLifecycleOwner, Observer {
            when (it) {
                is ResultWrapper.Error -> {
                    showVideoInfoErrorState()
                }
                is ResultWrapper.Success<*> -> {
                    hideVideoInfoErrorState()
                    videoItem = (it.data as Video).items[0]
                    setVideoInfo()
                }
            }
        })

        viewModel.favoriteVideoLiveData.observe(viewLifecycleOwner, Observer { isFavorite ->
            isVideoAddedToFavorite = isFavorite

            if (isVideoAddedToFavorite) {
                binding.babVideoDetails.menu.findItem(R.id.miFavoriteBabVideoDetails)
                    .setIcon(R.drawable.ic_favorite_filled)
            } else {
                binding.babVideoDetails.menu.findItem(R.id.miFavoriteBabVideoDetails)
                    .setIcon(R.drawable.ic_favorite_border)
            }
        })
    }

    private fun setVideoInfo() {
        with(videoItem) {
            binding.tvVideoTitleVideoDetails.text = snippet.title
            binding.tvViewCountVideoDetails.text =
                statistics.viewCount?.toLong()?.getFormattedNumberInString() ?: getString(
                    R.string.text_count_unavailable
                )
            binding.tvLikeCountVideoDetails.text =
                statistics.likeCount?.toLong()?.getFormattedNumberInString() ?: getString(
                    R.string.text_count_unavailable
                )
            binding.tvDislikeCountVideoDetails.text =
                statistics.dislikeCount?.toLong()?.getFormattedNumberInString() ?: getString(
                    R.string.text_count_unavailable
                )
            binding.tvCommentCountVideoDetails.text =
                statistics.commentCount.toLong().getFormattedNumberInString()
            binding.tvVideoDescVideoDetails.text = getString(
                R.string.text_video_description,
                DateTimeUtils.getPublishedDate(snippet.publishedAt),
                snippet.description
            )
        }
    }

    private fun showVideoInfoErrorState() {
        binding.groupInfoVideoDetails.makeGone()
        binding.groupErrorVideoDetails.makeVisible()
    }

    private fun hideVideoInfoErrorState() {
        binding.groupErrorVideoDetails.makeGone()
        binding.groupInfoVideoDetails.makeVisible()
    }


    private fun setupBottomAppBar() {
        binding.babVideoDetails.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.miFavoriteBabVideoDetails -> {
                    if (this::videoItem.isInitialized) {
                        // Add or remove from favorites only after videoItem details are fetched
                        isVideoAddedToFavorite = if (isVideoAddedToFavorite) {
                            item.setIcon(R.drawable.ic_favorite_border)
                            removeVideoFromFavorites()
                            false
                        } else {
                            // Add video to favorites
                            item.setIcon(R.drawable.ic_favorite_filled)
                            addVideoToFavorites()
                            true
                        }
                    } else {
                        context?.toast(getString(R.string.text_fetch_video_details_wait_msg))
                    }
                    true
                }
                R.id.miShareBabVideoDetails -> {
                    shareVideoUrl()
                    true
                }
                R.id.miOpenBabVideoDetails -> {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(getString(R.string.text_video_share_url, videoId))
                    )
                    startActivity(intent)
                    true
                }
                R.id.miPIPBabVideoDetails -> {
                    val player = activity
                    if (player is VideoPlayerActivity) {
                        player.enterPIP()
                    }
                    true
                }
                else -> false
            }

        }
    }

    private fun addVideoToFavorites() {
        val favoriteVideo = FavoritesEntry(
            videoId,
            videoItem.snippet.title, PLAYLIST_TYPE_VIDEO,
            videoItem.snippet.thumbnails.standard?.url ?: videoItem.snippet.thumbnails.high.url,
            System.currentTimeMillis(),
            true
        )
        viewModel.addVideoToFavorites(favoriteVideo)
    }

    private fun removeVideoFromFavorites() {
        val favoriteVideo = FavoritesEntry(
            videoId,
            videoItem.snippet.title, PLAYLIST_TYPE_VIDEO,
            videoItem.snippet.thumbnails.standard?.url ?: videoItem.snippet.thumbnails.high.url,
            System.currentTimeMillis(),
            true
        )
        viewModel.removeVideoFromFavorites(favoriteVideo)
    }

    private fun shareVideoUrl() {
        context?.startShareTextIntent(
            getString(R.string.text_share_video),
            getString(R.string.text_video_share_url, videoId)
        )
    }

    private fun onCommentsClick() {
        findNavController().navigate(
            VideoDetailsFragmentDirections.actionVideoDetailsFragmentToCommentsFragment(
                videoId
            )
        )
    }

    private fun onRetryClick() {
        fetchVideoInfo()
    }
}
