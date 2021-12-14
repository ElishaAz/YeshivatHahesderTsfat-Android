package tsfat.yeshivathahesder.channel.repository

import tsfat.yeshivathahesder.channel.db.FavoriteVideoDao
import tsfat.yeshivathahesder.channel.model.FavoriteVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FavoritesRepository(private val favoriteVideoDao: FavoriteVideoDao) {

    suspend fun getFavoriteVideosFromDb(): List<FavoriteVideo> = withContext(Dispatchers.IO) {
        favoriteVideoDao.getAllFavoriteVideos()
    }

    suspend fun removeVideoFromFavorites(favoriteVideo: FavoriteVideo) {
        withContext(Dispatchers.IO) {
            favoriteVideoDao.removeFavoriteVideo(favoriteVideo)
        }
    }

    suspend fun addVideoToFavorites(favoriteVideo: FavoriteVideo) {
        withContext(Dispatchers.IO) {
            favoriteVideoDao.addFavoriteVideo(favoriteVideo)
        }
    }
}