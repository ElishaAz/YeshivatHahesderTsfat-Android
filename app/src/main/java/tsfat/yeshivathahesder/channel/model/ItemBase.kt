package tsfat.yeshivathahesder.channel.model

abstract class ItemBase {
    abstract val id: String
    abstract val publishedAt: String

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ItemBase

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

}