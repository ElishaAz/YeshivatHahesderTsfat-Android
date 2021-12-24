package tsfat.yeshivathahesder.channel.db

import tsfat.yeshivathahesder.channel.model.FavoritesEntry
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface FavoriteVideoDao {

    @Insert
    suspend fun addFavoriteVideo(favoritesEntry: FavoritesEntry)

    @Delete
    suspend fun removeFavoriteVideo(favoritesEntry: FavoritesEntry)

    @Query("DELETE FROM favorite_videos where  id in (:idList)")
    suspend fun removeMultipleFavoriteVideos(idList: List<String>)

    @Query("SELECT id FROM favorite_videos WHERE id = :id LIMIT 1")
    suspend fun getFavoriteVideoId(id: String): String?

    @Query("SELECT * FROM favorite_videos ORDER BY timeInMillis DESC")
    suspend fun getAllFavoriteVideos(): List<FavoritesEntry>

    @Query("DELETE FROM favorite_videos")
    suspend fun removeAllFavoriteVideos()



}