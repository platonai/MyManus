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
    var attributes: String? = null,
    val isVisible: Boolean,
    val isInViewport: Boolean,
) {
    constructor(map: Map<String, Any?>) : this(
        index = AnyNumberConvertor(map["index"]).toIntOrNull() ?: -1,
        attributes = map["attributes"]?.toString()?.takeIf { it.isNotBlank() },
        vi = map["vi"] as String?,
        tagName = map["tagName"] as String,
        type = map["type"]?.toString()?.takeIf { it.isNotBlank() },
        role = map["role"]?.toString()?.takeIf { it.isNotBlank() },
        text = map["text"]?.toString()?.takeIf { it.isNotBlank() },
        value = map["value"]?.toString()?.takeIf { it.isNotBlank() },
        placeholder = map["placeholder"]?.toString()?.takeIf { it.isNotBlank() },
        name = map["name"]?.toString()?.takeIf { it.isNotBlank() },
        id = map["id"]?.toString()?.takeIf { it.isNotBlank() },
        ariaLabel = map["aria-label"]?.toString()?.takeIf { it.isNotBlank() },
        isVisible = map["isVisible"] as Boolean,
        isInViewport = map["isInViewport"] as Boolean
    )

    val description: String get() = text ?: value ?: placeholder ?: role ?: attributes ?: ""

    val brief: String get() = "$index | $tagName | $description | $attributes"
}
