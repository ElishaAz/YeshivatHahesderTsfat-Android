package tsfat.yeshivathahesder.channel.model

data class VideoItem(
    val contentDetails: ContentDetails,
    val snippet: Snippet
) : ItemBase() {
    data class ContentDetails(
        val videoId: String,
        val videoPublishedAt: String
    )

    data class Snippet(
        val thumbnails: Thumbnails,
        val title: String
    ) {
        data class Thumbnails(
            val default: Default,
            val high: High,
            val maxres: Maxres,
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

            data class Maxres(
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

    override val baseId: String
        get() = "YT-" + contentDetails.videoId

    override val publishedAt: String
        get() = contentDetails.videoPublishedAt
}