package tsfat.yeshivathahesder.channel.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_videos")
data class FavoritesEntry(
    @PrimaryKey val id: String,
    val title: String,
    val type: String,
    val thumbnail: String,
    val timeInMillis: Long,
    var isChecked: Boolean
)