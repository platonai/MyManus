package ai.platon.manus.common

class AnyNumberConvertor(
    val input: Any?
) {
    fun toIntOrNull(): Int? {
        return when (input) {
            is Short? -> input?.toInt()
            is Int? -> input
            is Long? -> input?.toInt()
            is String? -> input?.toIntOrNull()
            else -> null
        }
    }

    fun toLongOrNull(): Long? {
        return when (input) {
            is Short? -> input?.toLong()
            is Long? -> input
            is Int? -> input?.toLong()
            is String? -> input?.toLongOrNull()
            else -> null
        }
    }
}
