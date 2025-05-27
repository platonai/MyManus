package ai.platon.manus.tool

import ai.platon.manus.browser.waitForLoadState
import ai.platon.manus.common.AnyNumberConvertor
import ai.platon.manus.common.JS_GET_INTERACTIVE_ELEMENTS
import ai.platon.manus.common.JS_GET_SCROLL_INFO
import ai.platon.manus.tool.support.ToolExecuteResult
import ai.platon.pulsar.common.alwaysFalse
import ai.platon.pulsar.common.urls.URLUtils
import ai.platon.pulsar.protocol.browser.driver.cdt.PulsarWebDriver
import ai.platon.pulsar.protocol.browser.impl.DefaultBrowserFactory
import ai.platon.pulsar.skeleton.PulsarSettings
import ai.platon.pulsar.skeleton.context.PulsarContexts
import ai.platon.pulsar.skeleton.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
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
        SESSION.connect(driver)

        val action = args[PARAM_ACTION] as? String? ?: return ToolExecuteResult("Action parameter is required")
        val url = args[PARAM_URL]?.toString()
        val vi = args[PARAM_BOUNDING_BOX]?.toString()
        val text = args[PARAM_TEXT]?.toString()
        val script = args[PARAM_SCRIPT]?.toString()
        val scrollAmount = AnyNumberConvertor(args[PARAM_SCROLL_AMOUNT]).toIntOrNull()
        val tabId = AnyNumberConvertor(args[PARAM_TAB_ID]).toIntOrNull()

        try {
            when (action) {
                ACTION_NAVIGATE -> {
                    if (url == null) {
                        return ToolExecuteResult("URL is required | $ACTION_NAVIGATE")
                    }
                    SESSION.open(url, driver)
                    return ToolExecuteResult("Navigated to $url")
                }

                ACTION_CLICK -> {
                    if (vi == null) {
                        return ToolExecuteResult("Vi is required | $ACTION_CLICK")
                    }
                    driver.click("*[vi='$vi']")
                    driver.waitForLoadState("NETWORKIDLE")
                    return ToolExecuteResult("Clicked element at $vi")
                }

                ACTION_INPUT_TEXT -> {
                    if (vi == null || text == null) {
                        return ToolExecuteResult("Index and text are required | $ACTION_INPUT_TEXT")
                    }
                    driver.fill("*[vi='$vi']", text)
                    return ToolExecuteResult("Successfully input '$text' into element at $vi")
                }

                ACTION_KEY_ENTER -> {
                    if (vi == null) {
                        return ToolExecuteResult("Vi is required | $ACTION_KEY_ENTER")
                    }
                    driver.press("*[vi='$vi']", "Enter")
                    driver.waitForLoadState("NETWORKIDLE")
                    return ToolExecuteResult("Hit the enter key at $vi")
                }

                ACTION_SCREENSHOT -> {
                    val base64 = driver.captureScreenshot() ?: return ToolExecuteResult("Failed to capture screenshot")
                    return ToolExecuteResult("Screenshot captured (base64 length: ${base64.length})")
                }

                ACTION_GET_HTML -> {
                    val html = driver.outerHTML(":root") ?: ""
                    return ToolExecuteResult(StringUtils.abbreviate(html, MAX_HTML_LENGTH))
                }

                ACTION_GET_TEXT -> {
                    var body = driver.selectFirstTextOrNull("body") ?: ""

                    body = StringUtils.abbreviate(body, MAX_TEXT_LENGTH)

                    logger.info("get_text body: \n{}", body)
                    return ToolExecuteResult(body)
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
                    val newDriver = driver.browser.newDriver()
                    SESSION.open(url, newDriver)
                    return ToolExecuteResult("Opened new tab | $url")
                }

                ACTION_CLOSE_TAB -> {
                    driver.close()
                    return ToolExecuteResult("Closed current tab")
                }

                ACTION_SWITCH_TAB -> {
                    if (tabId == null) {
                        return ToolExecuteResult("Tab ID is required | $ACTION_SWITCH_TAB")
                    }
                    val drivers = driver.browser.drivers
                    ACTIVE_DRIVER = drivers[tabId.toString()] ?: return ToolExecuteResult("Tab ID not found | $tabId")
                    return ToolExecuteResult("Switched to tab | $tabId")
                }

                ACTION_REFRESH -> {
                    SESSION.open(driver.currentUrl(), driver)
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
        SESSION.connect(driver)

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
            // make sure the document is fully loaded
            driver.evaluateDetail("__pulsar_utils__.waitForReady()")
            // make sure all metadata are available
            driver.evaluateDetail("__pulsar_utils__.compute()")
        } catch (e: Exception) {
            logger.warn("Failed to compute the features of the document | {}", currentUrl)
        }

        try {
            // Viewport and scroll information
            val scrollInfo = driver.evaluateValue("($JS_GET_SCROLL_INFO)()") as Map<String, Any?>
            state[STATE_SCROLL_INFO] = scrollInfo
        } catch (e: Exception) {
            logger.warn("Failed to get scroll info via js | {}\n{}", currentUrl, JS_GET_SCROLL_INFO)
        }

        state[STATE_INTERACTIVE_ELEMENTS] = getInteractiveElements(currentUrl, state)

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
        state[STATE_HELP] = "Element description has the format: `index | bounding-box | type | text`, " +
                "use bounding-box to locate the corresponding the elements." +
                "Clicking them will navigate to or interact with their associated content."
    }

    // Add helper method for element selection
    private suspend fun getInteractiveElements(url: String, state: MutableMap<String, Any?>): String? {
        try {
            // Interactive elements
            val jsResult = driver.evaluateValueDetail("($JS_GET_INTERACTIVE_ELEMENTS)()")
            requireNotNull(jsResult) { "Js result must not be null - \n$JS_GET_INTERACTIVE_ELEMENTS" }
            val elementsInfo = jsResult.value as List<MutableMap<String, Any?>>
            elementsInfo.forEach { element ->
                // fix baidu.com's textarea issue
                if (element["tagName"] == "textarea") {
                    element["text"] = ""
                    element["value"] = ""
                }
            }

            val visibleInteractiveElements = elementsInfo
                .asSequence()
                .filter { it["isVisible"] == true }
                .filter { it["isInViewport"] == true }
                .onEach {
                    it["combinedText"] = it["text"] ?: it["value"] ?: it["placeholder"] ?: it["role"] ?: ""
                }.onEach {
                    // remove all non-printable characters
                    it["combinedText"] = StringUtils.abbreviate(it["combinedText"].toString(), 100)
                        .replace("[^\\p{Print}]".toRegex(), " ")
                        .replace("\\s+".toRegex(), " ")
                }.map {
                    it["index"].toString() + " | " + it["vi"] + " | " + it["tagName"] + " | " + it["combinedText"]
                }.joinToString("\n") { it.replace("\\s+", " ") }

            // state[STATE_INTERACTIVE_ELEMENTS] = visibleInteractiveElements

            return visibleInteractiveElements
        } catch (e: Exception) {
            logger.warn("Failed to get elements info via js | {} |\n{}", url, JS_GET_INTERACTIVE_ELEMENTS)
        }

        return null
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(BrowserUseTool::class.java)

        private const val MAX_HTML_LENGTH = 20000
        private const val MAX_TEXT_LENGTH = 10000

        val SESSION = PulsarContexts.getOrCreateSession()

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
