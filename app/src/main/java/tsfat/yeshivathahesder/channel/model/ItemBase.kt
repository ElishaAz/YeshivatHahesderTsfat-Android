package tsfat.yeshivathahesder.channel.model

abstract class ItemBase {
    abstract val baseId: String
    abstract val publishedAt: String

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ItemBase

        if (baseId != other.baseId) return false

        return true
    }

    override fun hashCode(): Int {
        return baseId.hashCode()
    }

}