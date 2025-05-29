package ai.platon.manus.tool

import ai.platon.manus.browser.*
import ai.platon.manus.common.*
import ai.platon.manus.tool.support.ToolExecuteResult
import ai.platon.pulsar.common.ResourceLoader
import ai.platon.pulsar.common.alwaysFalse
import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.urls.URLUtils
import ai.platon.pulsar.protocol.browser.driver.cdt.PulsarWebDriver
import ai.platon.pulsar.protocol.browser.impl.DefaultBrowserFactory
import ai.platon.pulsar.skeleton.PulsarSettings
import ai.platon.pulsar.skeleton.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.ai.tool.function.FunctionToolCallback
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

class BrowserUseTool() : AbstractTool() {
    private val closed = AtomicBoolean()

    val browser get() = BROWSER

    /**
     * The ordered drivers, each driver is associated with a tab.
     * */
    val drivers
        get() = driver.browser.drivers.values
            .filterIsInstance<AbstractWebDriver>()
            .filter { it.isActive }
            .sortedBy { it.id }

    /**
     * The current active driver.
     * */
    val driver: PulsarWebDriver get() = ACTIVE_DRIVER as PulsarWebDriver

    @get:Synchronized
    val currentState: Map<String, Any?> get() = runBlocking { computeCurrentState() }

    fun reset() {
        close()
        closed.set(false)
    }

    override fun run(args: Map<String, Any?>): ToolExecuteResult {
        logger.info("Using browser ... | {}", args)

        return runBlocking { run0(args) }
    }

    private suspend fun run0(args: Map<String, Any?>): ToolExecuteResult {
        val action = args[PARAM_ACTION] as? String? ?: return ToolExecuteResult("Action parameter is required")
        val url = args[PARAM_URL]?.toString()
        // The node index is actually is the nodeId return by CDP
        val index = AnyNumberConvertor(args[PARAM_INDEX]).toIntOrNull() ?: -1
        val text = args[PARAM_TEXT]?.toString()
        val script = args[PARAM_SCRIPT]?.toString()
        val scrollAmount = AnyNumberConvertor(args[PARAM_SCROLL_AMOUNT]).toIntOrNull()
        val tabId = AnyNumberConvertor(args[PARAM_TAB_ID]).toIntOrNull() ?: -1

        try {
            when (action) {
                ACTION_NAVIGATE -> {
                    if (url == null) {
                        return ToolExecuteResult("URL is required | $ACTION_NAVIGATE")
                    }

                    driver.navigateTo(url)
                    driver.waitForLoadState("NETWORKIDLE")

                    return ToolExecuteResult("Navigated to $url")
                }

                ACTION_GO_BACK -> {
                    driver.goBack()
                    driver.waitForLoadState("NETWORKIDLE")

                    return ToolExecuteResult("Navigated back to previous page")
                }

                ACTION_CLICK -> {
                    if (index < 0) {
                        return ToolExecuteResult("Index is required | $ACTION_CLICK")
                    }

                    debugClickableElements(index)

                    driver.click(index)
                    driver.delay(3000)
                    driver.waitForLoadState("NETWORKIDLE")

                    return ToolExecuteResult("Clicked element at #$index")
                }

                ACTION_INPUT_TEXT -> {
                    if (index < 0 || text == null) {
                        return ToolExecuteResult("Index and text are required | $ACTION_INPUT_TEXT")
                    }

                    driver.type(index, text)
                    return ToolExecuteResult("Successfully input '$text' into element at #$index")
                }

                ACTION_KEY_ENTER -> {
                    if (index < 0) {
                        return ToolExecuteResult("Index is required | $ACTION_KEY_ENTER")
                    }

                    driver.press(index, "Enter")
                    driver.waitForLoadState("NETWORKIDLE")

                    return ToolExecuteResult("Hit the enter key at #$index")
                }

                ACTION_SCREENSHOT -> {
                    val base64 = driver.captureScreenshot() ?: return ToolExecuteResult("Failed to capture screenshot")
                    return ToolExecuteResult("Screenshot captured (base64 length: ${base64.length})")
                }

                ACTION_GET_HTML -> {
                    val html = driver.outerHTML(":root") ?: ""
                    return ToolExecuteResult(StringUtils.abbreviate(html, MAX_LENGTH))
                }

                ACTION_GET_TEXT -> {
                    val textContent = driver.getTextContent()
                    logger.debug("get_text body is {}", textContent)
                    return ToolExecuteResult(textContent)
                }

                ACTION_EXECUTE_JS -> {
                    if (script == null) {
                        return ToolExecuteResult("Script is required | $ACTION_EXECUTE_JS")
                    }

                    val result = driver.evaluateValue(script)

                    return if (result == null) {
                        ToolExecuteResult("Successfully executed JavaScript code.")
                    } else {
                        ToolExecuteResult(result.toString())
                    }
                }

                ACTION_SCROLL -> {
                    if (scrollAmount == null) {
                        return ToolExecuteResult("Scroll amount is required | $ACTION_SCROLL")
                    }

                    if (scrollAmount > 0) {
                        driver.scrollDown(scrollAmount)
                    } else {
                        driver.scrollUp(abs(scrollAmount))
                    }

                    val direction = if (scrollAmount > 0) "down" else "up"
                    return ToolExecuteResult("Scrolled $direction by ${abs(scrollAmount.toDouble())} pixels")
                }

                ACTION_NEW_TAB -> {
                    if (url == null) {
                        return ToolExecuteResult("URL is required | $ACTION_NEW_TAB")
                    }

                    val newDriver = browser.newDriver()
                    newDriver.navigateTo(url)
                    driver.waitForLoadState("NETWORKIDLE")
                    return ToolExecuteResult("Opened new tab | $url")
                }

                ACTION_CLOSE_TAB -> {
                    driver.close()
                    return ToolExecuteResult("Closed current tab")
                }

                ACTION_SWITCH_TAB -> {
                    if (tabId < 0) {
                        return ToolExecuteResult("Tab ID is required | $ACTION_SWITCH_TAB")
                    }

                    ACTIVE_DRIVER = drivers[tabId]
                    return ToolExecuteResult("Switched to tab | $tabId")
                }

                ACTION_REFRESH -> {
                    driver.reload()
                    driver.waitForLoadState("NETWORKIDLE")
                    return ToolExecuteResult("Page refreshed")
                }

                else -> return ToolExecuteResult("Unknown action | $action")
            }
        } catch (e: Exception) {
            return ToolExecuteResult("Browser action failed | $action | ${e.message}")
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {

        }
    }

    private suspend fun computeCurrentState(): Map<String, Any?> {
        val state: MutableMap<String, Any?> = HashMap()

        try {
            computeCurrentStateTo(state)

            return state
        } catch (e: Exception) {
            logger.warn("Failed to get browser state", e)
            state[STATE_ERROR] = "Failed to get browser state: ${e.message}"
            return state
        }
    }

    private suspend fun computeCurrentStateTo(state: MutableMap<String, Any?>) {
        // Basic information
        val currentUrl = driver.currentUrl()

        val hasJsUtils = URLUtils.isStandard(currentUrl) && driver.evaluateValue("__pulsar_utils__.add(1, 1)") == 2
        val title = if (hasJsUtils) driver.selectFirstTextOrNull("title") else null

        state[STATE_URL] = currentUrl
        state[STATE_TITLE] = title

        // Tab information
        val tabs: List<Map<String, Any?>> = drivers.mapIndexed { i, it ->
            mapOf(
                STATE_URL to it.url(),
                STATE_TITLE to if (hasJsUtils) driver.selectFirstTextOrNull("title") else null,
                "id" to i
            )
        }

        state[STATE_TABS] = tabs

        // Not a normal page, e.g. about:blank
        if (!hasJsUtils) {
            return
        }

        try {
            // Viewport and scroll information
            val scrollInfo = driver.evaluateValue("($JS_GET_SCROLL_INFO)()") as Map<String, Any?>
            state[STATE_SCROLL_INFO] = scrollInfo
        } catch (e: Exception) {
            logger.warn("Failed to get scroll info via js | {}\n{}", currentUrl, JS_GET_SCROLL_INFO)
        }

        state[STATE_INTERACTIVE_ELEMENTS] = getInteractiveElements().joinToString("\n") { it.brief }

        // highlightInteractiveElements()

        try {
            // Capture screenshot
            if (alwaysFalse()) {
                val base64Screenshot = driver.captureScreenshot()
                state[STATE_SCREENSHOT] = base64Screenshot
            }
        } catch (e: Exception) {
            logger.warn("Failed to capture screenshot | {}", currentUrl)
        }

        // Add help information
        state[STATE_HELP] = "[0], [1], [2], etc., are clickable indices correspond to the listed elements." +
                "Clicking them will navigate to or interact with their associated content."
    }

    /**
     * Get interactive elements in the current page.
     *
     * The node IDs are used for node location because they do not change after DOM is loaded.
     * */
    private suspend fun getInteractiveElements(): List<ElementInfo> {
        try {
            // Interactive elements
            val jsResult = driver.evaluateValueDetail("($JS_GET_INTERACTIVE_ELEMENTS)()")
            requireNotNull(jsResult) { "Js result must not be null - \n$JS_GET_INTERACTIVE_ELEMENTS" }
            val elementsInfo = jsResult.value as List<MutableMap<String, Any?>>
            val nodeIds = getInteractiveElementsNodeId()
            elementsInfo.forEachIndexed { i, ele ->
                ele["index"] = nodeIds.getOrNull(i) // Add nodeId to each element info
                // fix baidu.com's textarea issue
                if (ele["tagName"] == "textarea") {
                    ele["text"] = ""
                    ele["value"] = ""
                }
            }

            val visibleElements = elementsInfo.map { ElementInfo(it) }
                .filter { it.isVisible && it.isInViewport }

            return visibleElements
        } catch (e: Exception) {
            logger.warn("Failed to get elements info via js |\n{}", e.message)
        }

        return emptyList()
    }

    /**
     * Get the node IDs of interactive elements.
     * */
    private suspend fun getInteractiveElementsNodeId(): List<Int> {
        val selector = BROWSER_INTERACTIVE_ELEMENTS_SELECTOR.trimIndent().trim()
        return driver.querySelectorAll(selector)
    }

    /**
     * Highlight interactive elements in the current page.
     * */
    private suspend fun highlightInteractiveElements() {
        // highlight interactive elements and return them, the return type is a map of nodeData, where the id is a
        // document scope sequence number, e.g. 0, 1, 2, etc.
        //     const nodeData = {
        //      tagName: node.tagName.toLowerCase(),
        //      attributes: {},
        //      xpath: getXPathTree(node, true),
        //      children: [],
        //    };
        try {
            val highlightJs = ResourceLoader.readString("js/build_dom_tree.js").trimEnd { it in " \n\r;" }
            // NOTE: The highlighted interactive elements are not actually used by MyManus,
            // The node IDs are used for node location because they do not change after DOM is loaded.
            driver.evaluateValue("($highlightJs)()")
        } catch (e: Exception) {
            logger.warn("Failed highlight interactive elements | {}", e.brief())
        }
    }

    private fun debugClickableElements(nodeId: Int) {
        try {
            val dom = driver.devTools.dom
            val attributes = dom.getAttributes(nodeId)
            if (attributes.isNotEmpty()) {
                println("Clicking element #$nodeId with attributes: " + attributes.joinToString(", "))
            } else {
                println("Clicking element #$nodeId with no attributes found | " + dom.outerHTML)
            }
        } catch (e: Exception) {
            logger.warn("Failed to get attributes for element #$nodeId | {}", e.message)
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(BrowserUseTool::class.java)

        private const val MAX_LENGTH = 20000

        var BROWSER = DefaultBrowserFactory().launchDefaultBrowser()

        var ACTIVE_DRIVER: WebDriver = BROWSER.newDriver()

        private const val NAME = "browser_use"

        private const val DESCRIPTION = BROWSER_USE_TOOL_DESCRIPTION

        private val PARAMETERS = BROWSER_USE_TOOL_PARAMETERS.trimIndent()

        init {
            // Enable Single Page Application (SPA) mode
            PulsarSettings().withSPA()
        }

        fun newDriver() {
            ACTIVE_DRIVER = BROWSER.newDriver()
        }

        val INSTANCE: BrowserUseTool by lazy { BrowserUseTool() }

        fun getFunctionToolCallback(): FunctionToolCallback<*, *> {
            return FunctionToolCallback.builder(NAME, INSTANCE)
                .description(DESCRIPTION)
                .inputSchema(PARAMETERS)
                .inputType(Map::class.java)
                .build()
        }

        fun close() {
            INSTANCE.close()
        }
    }
}
