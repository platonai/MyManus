package ai.platon.manus.common

data class ElementInfo(
    val index: Int,
    val vi: String?,
    val tagName: String,
    val type: String?,
    val role: String?,
    val text: String?,
    val value: String?,
    val placeholder: String?,
    val name: String?,
    val id: String?,
    val ariaLabel: String?,
    val isVisible: Boolean,
    val isInViewport: Boolean,
) {
    constructor(map: Map<String, Any?>) : this(
        index = map["index"] as Int,
        vi = map["vi"] as String?,
        tagName = map["tagName"] as String,
        type = map["type"] as String?,
        role = map["role"] as String?,
        text = map["text"] as String?,
        value = map["value"] as String?,
        placeholder = map["placeholder"] as String?,
        name = map["name"] as String?,
        id = map["id"] as String?,
        ariaLabel = map["aria-label"] as String?,
        isVisible = map["isVisible"] as Boolean,
        isInViewport = map["isInViewport"] as Boolean
    )

    val description: String get() = text ?: value ?: placeholder ?: role ?: ""

    val brief: String get() = "$index | $vi | $tagName | $description"
}
