package tsfat.yeshivathahesder.channel.db

import tsfat.yeshivathahesder.channel.model.FavoriteVideo
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [FavoriteVideo::class], version = 1)
abstract class ChannelifyDatabase : RoomDatabase() {

    abstract fun favoriteVideoDao(): FavoriteVideoDao
}