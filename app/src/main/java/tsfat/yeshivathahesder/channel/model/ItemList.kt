package tsfat.yeshivathahesder.channel.model


data class ItemList<T : ItemBase>(
    val nextPageToken: String?,
    val prevPageToken: String?,
    val items: List<T>
)