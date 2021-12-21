package tsfat.yeshivathahesder.channel.model


data class Playlists<T : PlaylistBase>(
    val items: List<T>,
    val nextPageToken: String?
) {
    data class VideoPlaylist(
        val contentDetails: ContentDetails,
        val id: String,
        val snippet: Snippet
    ) : PlaylistBase() {
        data class ContentDetails(
            val itemCount: Int
        )

        data class Snippet(
            val description: String,
            val publishedAt: String,
            val thumbnails: Thumbnails,
            val title: String
        ) {
            data class Thumbnails(
                val default: Default,
                val high: High,
                val medium: Medium,
                val standard: Standard?
            ) {
                data class Default(
                    val height: Int,
                    val url: String,
                    val width: Int
                )

                data class High(
                    val height: Int,
                    val url: String,
                    val width: Int
                )

                data class Medium(
                    val height: Int,
                    val url: String,
                    val width: Int
                )

                data class Standard(
                    val height: Int,
                    val url: String,
                    val width: Int
                )
            }
        }

        override val baseId: String = videoIdToBase(id)

        override val name: String
            get() = snippet.title
        override val itemCount: Int
            get() = contentDetails.itemCount
        override val publishedAt: String
            get() = snippet.publishedAt
    }
}