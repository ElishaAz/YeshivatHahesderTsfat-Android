package tsfat.yeshivathahesder.channel.model

abstract class PlaylistBase() {
    abstract val baseId: String
    abstract val name: String
    abstract val itemCount: Int
    abstract val publishedAt: String

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlaylistBase

        if (baseId != other.baseId) return false
        if (name != other.name) return false
        if (itemCount != other.itemCount) return false
        if (publishedAt != other.publishedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = baseId.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + itemCount
        result = 31 * result + publishedAt.hashCode()
        return result
    }


}