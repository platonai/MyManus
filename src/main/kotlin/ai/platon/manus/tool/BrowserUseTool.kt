package ai.platon.manus.tool

import ai.platon.manus.common.AnyNumberConvertor
import ai.platon.manus.common.BROWSER_INTERACTIVE_ELEMENTS_SELECTOR
import ai.platon.manus.common.JS_GET_INTERACTIVE_ELEMENTS
import ai.platon.manus.common.JS_GET_SCROLL_INFO
import ai.platon.manus.tool.support.ToolExecuteResult
import ai.platon.pulsar.common.warnForClose
import com.microsoft.playwright.*
import com.microsoft.playwright.impl.TargetClosedError
import com.microsoft.playwright.options.LoadState
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.ai.tool.function.FunctionToolCallback
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

// Action constants
const val ACTION_NAVIGATE = "navigate"
const val ACTION_CLICK = "click"
const val ACTION_INPUT_TEXT = "input_text"
const val ACTION_KEY_ENTER = "key_enter"
const val ACTION_SCREENSHOT = "screenshot"
const val ACTION_GET_HTML = "get_html"
const val ACTION_GET_TEXT = "get_text"
const val ACTION_EXECUTE_JS = "execute_js"
const val ACTION_SCROLL = "scroll"
const val ACTION_SWITCH_TAB = "switch_tab"
const val ACTION_NEW_TAB = "new_tab"
const val ACTION_CLOSE_TAB = "close_tab"
const val ACTION_REFRESH = "refresh"

// Parameter constants
const val PARAM_ACTION = "action"
const val PARAM_URL = "url"
const val PARAM_INDEX = "index"
const val PARAM_TEXT = "text"
const val PARAM_SCRIPT = "script"
const val PARAM_SCROLL_AMOUNT = "scroll_amount"
const val PARAM_TAB_ID = "tab_id"

// State key constants
const val STATE_URL = "url"
const val STATE_TITLE = "title"
const val STATE_TABS = "tabs"
const val STATE_SCROLL_INFO = "scroll_info"
const val STATE_INTERACTIVE_ELEMENTS = "interactive_elements"
const val STATE_SCREENSHOT = "screenshot"
const val STATE_HELP = "help"
const val STATE_ERROR = "error"

// Scroll info constants
const val SCROLL_PIXELS_ABOVE = "pixels_above"
const val SCROLL_PIXELS_BELOW = "pixels_below"
const val SCROLL_TOTAL_HEIGHT = "total_height"
const val SCROLL_VIEWPORT_HEIGHT = "viewport_height"

// Element info constants
const val ELEMENT_INDEX = "index"
const val ELEMENT_TAG_NAME = "tagName"
const val ELEMENT_TYPE = "type"
const val ELEMENT_ROLE = "role"
const val ELEMENT_TEXT = "text"
const val ELEMENT_VALUE = "value"
const val ELEMENT_PLACEHOLDER = "placeholder"
const val ELEMENT_NAME = "name"
const val ELEMENT_ID = "id"
const val ELEMENT_ARIA_LABEL = "aria-label"
const val ELEMENT_IS_VISIBLE = "isVisible"

const val BROWSER_USE_TOOL_DESCRIPTION = """
Automate web browser interactions including visiting pages, clicking elements, extracting content, and managing tabs.

You can perform these core actions:

- '$ACTION_NAVIGATE': Go to a specific URL
- '$ACTION_CLICK': Click an element by index
- '$ACTION_INPUT_TEXT': Input text into an element
- '$ACTION_KEY_ENTER': Hit the Enter key
- '$ACTION_SCREENSHOT': Capture a screenshot
- '$ACTION_GET_HTML': Get page HTML content
- '$ACTION_GET_TEXT': Get text content of the page
- '$ACTION_EXECUTE_JS': Execute JavaScript code
- '$ACTION_SCROLL': Scroll the page
- '$ACTION_SWITCH_TAB': Switch to a specific tab
- '$ACTION_NEW_TAB': Open a new tab
- '$ACTION_CLOSE_TAB': Close the current tab
- '$ACTION_REFRESH': Refresh the current page
"""

const val BROWSER_USE_TOOL_PARAMETERS = """
{
    "type": "object",
    "properties": {
        "$PARAM_ACTION": {
            "type": "string",
            "enum": [
                "$ACTION_NAVIGATE",
                "$ACTION_CLICK",
                "$ACTION_INPUT_TEXT",
                "$ACTION_KEY_ENTER",
                "$ACTION_SCREENSHOT",
                "$ACTION_GET_HTML",
                "$ACTION_GET_TEXT",
                "$ACTION_EXECUTE_JS",
                "$ACTION_SCROLL",
                "$ACTION_SWITCH_TAB",
                "$ACTION_NEW_TAB",
                "$ACTION_CLOSE_TAB",
                "$ACTION_REFRESH"
            ],
            "description": "The browser action to perform"
        },
        "$PARAM_URL": {
            "type": "string",
            "description": "URL for '$ACTION_NAVIGATE' or '$ACTION_NEW_TAB' actions"
        },
        "$PARAM_INDEX": {
            "type": "integer",
            "description": "Element index for '$ACTION_CLICK' or '$ACTION_INPUT_TEXT' actions"
        },
        "$PARAM_TEXT": {
            "type": "string",
            "description": "Text for '$ACTION_INPUT_TEXT' action"
        },
        "$PARAM_SCRIPT": {
            "type": "string",
            "description": "JavaScript code for '$ACTION_EXECUTE_JS' action"
        },
        "$PARAM_SCROLL_AMOUNT": {
            "type": "integer",
            "description": "Pixels to scroll (positive for down, negative for up) for '$ACTION_SCROLL' action"
        },
        "$PARAM_TAB_ID": {
            "type": "integer",
            "description": "Tab ID for '$ACTION_SWITCH_TAB' action"
        }
    },
    "required": [
        "$PARAM_ACTION"
    ],
    "dependencies": {
        "$ACTION_NAVIGATE": [
            "$PARAM_URL"
        ],
        "$ACTION_CLICK": [
            "$PARAM_INDEX"
        ],
        "$ACTION_INPUT_TEXT": [
            "$PARAM_INDEX",
            "$PARAM_TEXT"
        ],
        "$ACTION_KEY_ENTER": [
            "$PARAM_INDEX"
        ],
        "$ACTION_EXECUTE_JS": [
            "$PARAM_SCRIPT"
        ],
        "$ACTION_SWITCH_TAB": [
            "$PARAM_TAB_ID"
        ],
        "$ACTION_NEW_TAB": [
            "$PARAM_URL"
        ],
        "$ACTION_SCROLL": [
            "$PARAM_SCROLL_AMOUNT"
        ]
    }
}
"""

class BrowserUseTool : AbstractTool() {
    private val closed = AtomicBoolean()

    private var headless = false
    private lateinit var browser: Browser
    private lateinit var context: BrowserContext
    lateinit var page: Page

    @get:Synchronized
    val currentState: Map<String, Any?> get() = computeCurrentState()

    fun config(headless: Boolean = false) {
        this.headless = headless
    }

    fun reset() {
        close()
        closed.set(false)
    }

    override fun run(args: Map<String, Any?>): ToolExecuteResult {
        logger.info("Using browser ... | {}", args)

        initializeBrowserIfNecessary()

        val action = args[PARAM_ACTION] as? String? ?: return ToolExecuteResult("Action parameter is required")
        val url = args[PARAM_URL]?.toString()
        val index = AnyNumberConvertor(args[PARAM_INDEX]).toIntOrNull()
        val text = args[PARAM_TEXT]?.toString()
        val script = args[PARAM_SCRIPT]?.toString()
        val scrollAmount = AnyNumberConvertor(args[PARAM_SCROLL_AMOUNT]).toIntOrNull()
        val tabId = AnyNumberConvertor(args[PARAM_TAB_ID]).toIntOrNull()

        val interactiveElements = getInteractiveElements()

        try {
            when (action) {
                ACTION_NAVIGATE -> {
                    if (url == null) {
                        return ToolExecuteResult("URL is required | $ACTION_NAVIGATE")
                    }
                    page.navigate(url)
                    page.waitForLoadState(LoadState.NETWORKIDLE)
                    return ToolExecuteResult("Navigated to $url")
                }

                ACTION_CLICK -> {
                    if (index == null) {
                        return ToolExecuteResult("Index is required | $ACTION_CLICK")
                    }
                    if (index < 0 || index >= interactiveElements.size) {
                        return ToolExecuteResult("Element #$index not found")
                    }
                    interactiveElements[index].click()
                    page.waitForLoadState(LoadState.NETWORKIDLE)
                    return ToolExecuteResult("Clicked element at #$index")
                }

                ACTION_INPUT_TEXT -> {
                    if (index == null || text == null) {
                        return ToolExecuteResult("Index and text are required | $ACTION_INPUT_TEXT")
                    }
                    val elements = page.querySelectorAll("input, textarea")
                    if (index < 0 || index >= elements.size) {
                        return ToolExecuteResult("Element #$index not found")
                    }
                    elements[index].fill(text)
                    return ToolExecuteResult("Successfully input '$text' into element at #$index")
                }

                ACTION_KEY_ENTER -> {
                    if (index == null) {
                        return ToolExecuteResult("Index is required | $ACTION_KEY_ENTER")
                    }
                    if (index < 0 || index >= interactiveElements.size) {
                        return ToolExecuteResult("Element #$index not found")
                    }
                    interactiveElements[index].press("Enter")
                    page.waitForLoadState(LoadState.NETWORKIDLE)
                    return ToolExecuteResult("Hit the enter key at #$index")
                }

                ACTION_SCREENSHOT -> {
                    val screenshot = page.screenshot()
                    val base64 = Base64.getEncoder().encodeToString(screenshot)
                    return ToolExecuteResult("Screenshot captured (base64 length: ${base64.length})")
                }

                ACTION_GET_HTML -> {
                    val html = page.content()
                    return ToolExecuteResult(
                        if (html.length > MAX_LENGTH) html.substring(0, MAX_LENGTH) + "..." else html
                    )
                }

                ACTION_GET_TEXT -> {
                    val body = page.textContent("body")
                    logger.debug("get_text body is {}", body)
                    return ToolExecuteResult(body)
                }

                ACTION_EXECUTE_JS -> {
                    if (script == null) {
                        return ToolExecuteResult("Script is required | $ACTION_EXECUTE_JS")
                    }
                    val result = page.evaluate(script)
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
                    page.evaluate("window.scrollBy(0, $scrollAmount);")
                    val direction = if (scrollAmount > 0) "down" else "up"
                    return ToolExecuteResult("Scrolled $direction by ${abs(scrollAmount.toDouble())} pixels")
                }

                ACTION_NEW_TAB -> {
                    if (url == null) {
                        return ToolExecuteResult("URL is required | $ACTION_NEW_TAB")
                    }
                    val newPage = context.newPage()
                    newPage.navigate(url)
                    newPage.waitForLoadState(LoadState.NETWORKIDLE)
                    return ToolExecuteResult("Opened new tab | $url")
                }

                ACTION_CLOSE_TAB -> {
                    page.close()
                    return ToolExecuteResult("Closed current tab")
                }

                ACTION_SWITCH_TAB -> {
                    if (tabId == null) {
                        return ToolExecuteResult("Tab ID is required | $ACTION_SWITCH_TAB")
                    }
                    val pages = context.pages()
                    if (tabId < 0 || tabId >= pages.size) {
                        return ToolExecuteResult("Tab ID not found | $tabId")
                    }
                    page = pages[tabId]
                    return ToolExecuteResult("Switched to tab | $tabId")
                }

                ACTION_REFRESH -> {
                    page.reload()
                    page.waitForLoadState(LoadState.NETWORKIDLE)
                    return ToolExecuteResult("Page refreshed")
                }

                else -> return ToolExecuteResult("Unknown action | $action")
            }
        } catch (e: TimeoutError) {
            return ToolExecuteResult("Operation timed out | ${e.message}")
        } catch (e: TargetClosedError) {
            return ToolExecuteResult("Browser window was closed | ${e.message}")
        } catch (e: Exception) {
            return ToolExecuteResult("Browser action failed | $action | ${e.message}")
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            try {
                if (::page.isInitialized) {
                    page.close()
                }
                if (::context.isInitialized) {
                    context.close()
                }
                if (::browser.isInitialized) {
                    browser.close()
                }
            } catch (e: Exception) {
                warnForClose(this, e)
            }
        }
    }

    fun initializeBrowserIfNecessary() {
        if (!::browser.isInitialized) {
            browser = playwright.chromium().launch(BrowserType.LaunchOptions().setHeadless(headless))
        }
        if (!::context.isInitialized) {
            context = browser.newContext(
                Browser.NewContextOptions()
                    .setViewportSize(1920, 1080)
                    .setJavaScriptEnabled(true)
            )
        }
        if (!::page.isInitialized) {
            page = context.newPage()
        }

        if (page.isClosed) {
            logger.info("Page is closed, creating a new one")
            page = context.newPage()
        }
    }

    private fun computeCurrentState(): Map<String, Any?> {
        val state: MutableMap<String, Any?> = HashMap()

        try {
            initializeBrowserIfNecessary()
            computeCurrentStateTo(state)

            return state
        } catch (e: Exception) {
            logger.warn("Failed to get browser state", e)
            state[STATE_ERROR] = "Failed to get browser state: ${e.message}"
            return state
        }
    }

    private fun isInitialized(): Boolean {
        return ::context.isInitialized && ::browser.isInitialized && ::page.isInitialized
    }

    private fun computeCurrentStateTo(state: MutableMap<String, Any?>) {
        if (!isInitialized()) {
            return
        }
        if (page.isClosed || !browser.isConnected) {
            return
        }

        // Basic information
        val currentUrl = page.url()
        val title = page.title()

        state[STATE_URL] = currentUrl
        state[STATE_TITLE] = title

        // Tab information
        val pages = context.pages()
        val tabs: List<Map<String, Any?>> = pages.mapIndexed { i, it ->
            mapOf(
                STATE_URL to it.url(),
                STATE_TITLE to it.title(),
                "id" to i
            )
        }

        state[STATE_TABS] = tabs

        try {
            // Viewport and scroll information
            val scrollInfo = page.evaluate(JS_GET_SCROLL_INFO) as Map<String, Any?>
            state[STATE_SCROLL_INFO] = scrollInfo
        } catch (e: Exception) {
            logger.warn("Failed to get scroll info via js | {}\n{}", currentUrl, JS_GET_SCROLL_INFO)
        }

        try {
            // Interactive elements
            val jsResult = page.evaluate(JS_GET_INTERACTIVE_ELEMENTS)
            requireNotNull(jsResult) { "Js result must not be null - \n$JS_GET_INTERACTIVE_ELEMENTS" }
            val elementsInfo = jsResult as List<Map<String, Any?>>
            state[STATE_INTERACTIVE_ELEMENTS] = elementsInfo
        } catch (e: Exception) {
            logger.warn("Failed to get elements info via js | {} |\n{}", currentUrl, JS_GET_INTERACTIVE_ELEMENTS)
        }

        try {
            // Capture screenshot
            val screenshot = page.screenshot()
            val base64Screenshot = Base64.getEncoder().encodeToString(screenshot)
            state[STATE_SCREENSHOT] = base64Screenshot
        } catch (e: Exception) {
            logger.warn("Failed to capture screenshot | {}", currentUrl)
        }

        // Add help information
        state[STATE_HELP] = "[0], [1], [2], etc., are clickable indices correspond to the listed elements." +
                "Clicking them will navigate to or interact with their associated content."
    }

    // Add helper method for element selection
    private fun getInteractiveElements(): List<ElementHandle> {
        return page.querySelectorAll(BROWSER_INTERACTIVE_ELEMENTS_SELECTOR.trimIndent()).filter { it.isVisible }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(BrowserUseTool::class.java)
        private const val MAX_LENGTH = 20000

        private val playwright = Playwright.create()

        private val name = "browser_use"

        private val description = BROWSER_USE_TOOL_DESCRIPTION.trimIndent()

        private val PARAMETERS = BROWSER_USE_TOOL_PARAMETERS.trimIndent()

        /**
         * TODO: improve initialization
         * */
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
