package tsfat.yeshivathahesder.channel.viewmodel

import tsfat.yeshivathahesder.channel.R
import tsfat.yeshivathahesder.channel.model.FavoritesEntry
import tsfat.yeshivathahesder.channel.repository.VideoDetailsRepository
import tsfat.yeshivathahesder.core.helper.ResultWrapper
import android.content.Context
import androidx.lifecycle.*
import kotlinx.coroutines.launch

class VideoDetailsViewModel(
    private val videoDetailsRepository: VideoDetailsRepository,
    private val context: Context
) : ViewModel() {

    private val _videoInfoLiveData = MutableLiveData<ResultWrapper>()
    val videoInfoLiveData: LiveData<ResultWrapper>
        get() = _videoInfoLiveData

    private val _favoriteVideoLiveData = MutableLiveData<Boolean>()
    val favoriteVideoLiveData: LiveData<Boolean>
        get() = _favoriteVideoLiveData

    fun getVideoInfo(videoId: String) {
        viewModelScope.launch {
            _videoInfoLiveData.value = ResultWrapper.Loading

            val response = videoDetailsRepository.getVideoInfo(videoId)
            if (response.isSuccessful) {
                _videoInfoLiveData.value = ResultWrapper.Success(response.body())
            } else {
                _videoInfoLiveData.value =
                    ResultWrapper.Error(context.getString(R.string.error_video_details))
            }
        }
    }

    fun addVideoToFavorites(favoritesEntry: FavoritesEntry) {
        viewModelScope.launch {
            videoDetailsRepository.addVideoToFavorites(favoritesEntry)
        }
    }

    fun removeVideoFromFavorites(favoritesEntry: FavoritesEntry) {
        viewModelScope.launch {
            videoDetailsRepository.removeVideoFromFavorites(favoritesEntry)
        }
    }

    fun getVideoFavoriteStatus(videoId: String) {
        viewModelScope.launch {
            _favoriteVideoLiveData.value =  videoDetailsRepository.isVideoAddedToFavorites(videoId)
        }
    }
}