package tsfat.yeshivathahesder.channel.repository

import tsfat.yeshivathahesder.channel.api.VideosService
import tsfat.yeshivathahesder.channel.db.FavoriteVideoDao
import tsfat.yeshivathahesder.channel.model.FavoriteVideo
import tsfat.yeshivathahesder.channel.model.Video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class VideoDetailsRepository(
    private val videosService: VideosService,
    private val favoriteVideoDao: FavoriteVideoDao
) {

    suspend fun getVideoInfo(videoId: String): Response<Video> =
        videosService.getVideoInfo(videoId)

    suspend fun addVideoToFavorites(favoriteVideo: FavoriteVideo) {
        withContext(Dispatchers.IO) {
            favoriteVideoDao.addFavoriteVideo(favoriteVideo)
        }
    }

    suspend fun removeVideoFromFavorites(favoriteVideo: FavoriteVideo) {
        withContext(Dispatchers.IO) {
            favoriteVideoDao.removeFavoriteVideo(favoriteVideo)
        }
    }

    suspend fun isVideoAddedToFavorites(videoId: String): Boolean = withContext(Dispatchers.IO) {
        favoriteVideoDao.getFavoriteVideoId(videoId) != null
    }

}