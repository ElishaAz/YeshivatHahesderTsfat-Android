package tsfat.yeshivathahesder.channel.repository

import tsfat.yeshivathahesder.channel.db.FavoriteVideoDao
import tsfat.yeshivathahesder.channel.model.FavoritesEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FavoritesRepository(private val favoriteVideoDao: FavoriteVideoDao) {

    suspend fun getFavoriteVideosFromDb(): List<FavoritesEntry> = withContext(Dispatchers.IO) {
        favoriteVideoDao.getAllFavoriteVideos()
    }

    suspend fun removeVideoFromFavorites(favoritesEntry: FavoritesEntry) {
        withContext(Dispatchers.IO) {
            favoriteVideoDao.removeFavoriteVideo(favoritesEntry)
        }
    }

    suspend fun addVideoToFavorites(favoritesEntry: FavoritesEntry) {
        withContext(Dispatchers.IO) {
            favoriteVideoDao.addFavoriteVideo(favoritesEntry)
        }
    }
}