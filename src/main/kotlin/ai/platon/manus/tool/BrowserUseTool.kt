package ai.platon.manus.tool

import ai.platon.manus.browser.*
import ai.platon.manus.common.AnyNumberConvertor
import ai.platon.manus.common.BROWSER_INTERACTIVE_ELEMENTS_SELECTOR
import ai.platon.manus.common.JS_GET_INTERACTIVE_ELEMENTS
import ai.platon.manus.common.JS_GET_SCROLL_INFO
import ai.platon.manus.tool.support.ToolExecuteResult
import ai.platon.pulsar.protocol.browser.driver.cdt.PulsarWebDriver
import ai.platon.pulsar.protocol.browser.impl.DefaultBrowserFactory
import ai.platon.pulsar.skeleton.PulsarSettings
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
                    driver.delay(3000)
                    return ToolExecuteResult("Navigated to $url")
                }

                ACTION_CLICK -> {
                    if (index < 0) {
                        return ToolExecuteResult("Index is required | $ACTION_CLICK")
                    }

                    val interactiveElements = getInteractiveElements()
                    driver.click(interactiveElements[index])
                    driver.waitForLoadState("NETWORKIDLE")
                    driver.delay(3000)
                    return ToolExecuteResult("Clicked element at #$index")
                }

                ACTION_INPUT_TEXT -> {
                    if (index < 0 || text == null) {
                        return ToolExecuteResult("Index and text are required | $ACTION_INPUT_TEXT")
                    }

                    val interactiveElements = getInteractiveElements()
                    driver.fill(interactiveElements[index], text)
                    return ToolExecuteResult("Successfully input '$text' into element at #$index")
                }

                ACTION_KEY_ENTER -> {
                    if (index < 0) {
                        return ToolExecuteResult("Index is required | $ACTION_KEY_ENTER")
                    }

                    val interactiveElements = getInteractiveElements()
                    driver.press(interactiveElements[index], "Enter")
                    driver.waitForLoadState("NETWORKIDLE")
                    driver.delay(3000)
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

                    val newDriver = driver.browser.newDriver()
                    newDriver.navigateTo(url)
                    driver.waitForLoadState("NETWORKIDLE")
                    driver.delay(1000)
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

                    val drivers = driver.browser.drivers.values
                        .filterIsInstance<AbstractWebDriver>()
                        .filter { it.isActive }
                        .sortedBy { it.id }
                    ACTIVE_DRIVER = drivers[tabId]
                    return ToolExecuteResult("Switched to tab | $tabId")
                }

                ACTION_REFRESH -> {
                    driver.reload()
                    driver.waitForLoadState("NETWORKIDLE")
                    driver.delay(3000)
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
        val title = driver.selectFirstTextOrNull("title") ?: ""

        state[STATE_URL] = currentUrl
        state[STATE_TITLE] = title

        // Tab information
        val drivers = driver.browser.drivers.values
        val tabs: List<Map<String, Any?>> = drivers.mapIndexed { i, it ->
            mapOf(
                STATE_URL to it.url(),
                STATE_TITLE to driver.selectFirstTextOrNull("title"),
                "id" to i
            )
        }

        state[STATE_TABS] = tabs

        try {
            // Viewport and scroll information
            val scrollInfo = driver.evaluate(JS_GET_SCROLL_INFO) as Map<String, Any?>
            state[STATE_SCROLL_INFO] = scrollInfo
        } catch (e: Exception) {
            logger.warn("Failed to get scroll info via js | {}\n{}", currentUrl, JS_GET_SCROLL_INFO)
        }

        try {
            // Interactive elements
            val jsResult = driver.evaluateValueDetail(JS_GET_INTERACTIVE_ELEMENTS)
            requireNotNull(jsResult) { "Js result must not be null - \n$JS_GET_INTERACTIVE_ELEMENTS" }
            val elementsInfo = jsResult.value as List<Map<String, Any?>>
            state[STATE_INTERACTIVE_ELEMENTS] = elementsInfo
        } catch (e: Exception) {
            logger.warn("Failed to get elements info via js | {} |\n{}", currentUrl, JS_GET_INTERACTIVE_ELEMENTS)
        }

        try {
            // Capture screenshot
            val base64Screenshot = driver.captureScreenshot()
            state[STATE_SCREENSHOT] = base64Screenshot
        } catch (e: Exception) {
            logger.warn("Failed to capture screenshot | {}", currentUrl)
        }

        // Add help information
        state[STATE_HELP] = "[0], [1], [2], etc., are clickable indices correspond to the listed elements." +
                "Clicking them will navigate to or interact with their associated content."
    }

    // Add helper method for element selection
    private suspend fun getInteractiveElements(): List<Int> {
        val selector = BROWSER_INTERACTIVE_ELEMENTS_SELECTOR.trimIndent().trim()
        return driver.querySelectorAll(selector)
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(BrowserUseTool::class.java)
        private const val MAX_LENGTH = 20000

        var BROWSER = DefaultBrowserFactory().launchDefaultBrowser()

        var ACTIVE_DRIVER: WebDriver = BROWSER.newDriver()

        private val name = "browser_use"

        private val description = BROWSER_USE_TOOL_DESCRIPTION.trimIndent()

        private val PARAMETERS = BROWSER_USE_TOOL_PARAMETERS.trimIndent()

        init {
            PulsarSettings().withSPA()
        }

        fun newDriver() {
            ACTIVE_DRIVER = BROWSER.newDriver()
        }

        val INSTANCE: BrowserUseTool by lazy { BrowserUseTool() }

        fun getFunctionToolCallback(): FunctionToolCallback<*, *> {
            return FunctionToolCallback.builder(name, INSTANCE)
                .description(description)
                .inputSchema(PARAMETERS)
                .inputType(Map::class.java)
                .build()
        }

        fun close() {
            INSTANCE.close()
        }
    }
}
