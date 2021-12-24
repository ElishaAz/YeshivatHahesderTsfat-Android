package tsfat.yeshivathahesder.channel.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_videos")
data class FavoriteEntry(@PrimaryKey val id: String,
                         val title: String,
                         val thumbnail: String,
                         val timeInMillis: Long,
                         val type: String,
                         var isChecked: Boolean)