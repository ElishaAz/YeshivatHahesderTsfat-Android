package tsfat.yeshivathahesder.channel.db

import tsfat.yeshivathahesder.channel.model.FavoritesEntry
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [FavoritesEntry::class], version = 1)
abstract class ChannelifyDatabase : RoomDatabase() {

    abstract fun favoriteVideoDao(): FavoriteVideoDao
}