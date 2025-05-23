package ai.platon.manus.browser

import ai.platon.pulsar.browser.driver.chrome.ClickableDOM
import ai.platon.pulsar.browser.driver.chrome.PageHandler
import ai.platon.pulsar.browser.driver.chrome.util.ChromeDriverException
import ai.platon.pulsar.common.math.geometric.OffsetD
import ai.platon.pulsar.protocol.browser.driver.cdt.PulsarWebDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.random.Random
import kotlin.random.nextInt

class NativeWebDriver(private val driver: PulsarWebDriver) {
    private val pageAPI = driver.implementation.page
    private val domAPI = driver.implementation.dom

    val pageHandler = PageHandler(driver.implementation, driver.settings.confuser)
    val mouse = pageHandler.mouse
    val keyboard = pageHandler.keyboard

    private val delayPolicy by lazy { driver.browser.settings.interactSettings.generateRestrictedDelayPolicy() }

    suspend fun reload(ignoreCache: Boolean = false, scriptToEvaluateOnLoad: String? = null) {
        withContext(Dispatchers.IO) {
            pageAPI.reload(ignoreCache, scriptToEvaluateOnLoad)
        }
    }

    suspend fun setDocumentContent(htmlContent: String) {
        // escape htmlContent properly
        val escapedHtmlContent = htmlContent.replace("'", "\\'").replace("\"", "\\\"")
        val jsCode = """
                (function() {
                    var doc = document;
                    var html = '$escapedHtmlContent';
                    doc.open();
                    doc.write(html);
                    doc.close();
                })();
            """.trimIndent()

        withContext(Dispatchers.IO) {
            driver.evaluate(jsCode)
            // page.setDocumentContent(frameId, htmlContent)
        }
    }

    suspend fun querySelectorAll(selector: String): List<Int> {
        val nodeId = domAPI.document.nodeId
        return withContext(Dispatchers.IO) {
            domAPI.querySelectorAll(nodeId, selector)
        }
    }

    suspend fun waitForLoadState(loadState: String) {
        val state = loadState.lowercase()
        when (state) {
            "load" -> {

            }

            "domcontentloaded" -> {

            }

            "networkidle" -> {

            }

            else -> throw ChromeDriverException("Unsupported load state: $loadState")
        }
    }

    suspend fun click(nodeId: Int) {
        click(nodeId, 1)
    }

    suspend fun press(nodeId: Int, key: String) {
        click(nodeId, 1)
        keyboard.press(key, randomDelayMillis("press"))
    }

    suspend fun type(nodeId: Int, text: String) {
        click(nodeId, 1)
        keyboard.type(text, randomDelayMillis("type"))
        delay(200)
    }

    suspend fun fill(nodeId: Int, text: String) {
        val value = pageHandler.getAttribute(nodeId, "value")
        if (value != null) {
            // it's an input element, we should click on the right side of the element,
            // so the cursor appears at the tail of the text
            click(nodeId, 1, "right")
            keyboard.delete(value.length, randomDelayMillis("delete"))
            // ensure the input is empty
            // page.setAttribute(nodeId, "value", "")
        }

        click(nodeId, 1)
        // For fill, there is no delay between key presses
        keyboard.type(text, 0)
    }

    private suspend fun click(nodeId: Int, count: Int, position: String = "center") {
        val deltaX = 4.0 + Random.nextInt(4)
        val deltaY = 4.0
        val offset = OffsetD(deltaX, deltaY)
        val minDeltaX = 2.0

        val p = pageAPI
        val d = domAPI
        if (p == null || d == null) {
            return
        }

        val clickableDOM = ClickableDOM(p, d, nodeId, offset)
        val point = clickableDOM.clickablePoint().value ?: return
        val box = clickableDOM.boundingBox()
        val width = box?.width ?: 0.0
        // if it's an input element, we should click on the right side of the element,
        // so the cursor is at the tail of the text
        var offsetX = when (position) {
            "left" -> 0.0 + deltaX
            "right" -> width - deltaX
            else -> width / 2 + deltaX
        }
        offsetX = offsetX.coerceAtMost(width - minDeltaX).coerceAtLeast(minDeltaX)

        point.x += offsetX

        pageHandler.mouse.click(point.x, point.y, count, 200)
    }

    fun randomDelayMillis(action: String, fallback: IntRange = 500..1000): Long {
        val default = delayPolicy["default"] ?: fallback
        var range = delayPolicy[action] ?: default

        if (range.first <= 0 || range.last > 10000) {
            range = fallback
        }

        return Random.nextInt(range).toLong()
    }
}

val PulsarWebDriver.native get() = NativeWebDriver(this)

suspend fun PulsarWebDriver.reload() {
    native.reload()
}

suspend fun PulsarWebDriver.setContent(htmlContent: String) {
    native.setDocumentContent(htmlContent)
}

suspend fun PulsarWebDriver.getTextContent(): String {
    return withContext(Dispatchers.IO) {
        evaluateValue("document.documentElement.innerText")?.toString() ?: ""
    }
}

suspend fun PulsarWebDriver.getTextContent(selector: String): String {
    return withContext(Dispatchers.IO) {
        evaluateValue("document.querySelector('$selector').innerText")?.toString() ?: ""
    }
}

suspend fun PulsarWebDriver.waitForLoadState(loadState: String) {
    native.waitForLoadState(loadState)
}

suspend fun PulsarWebDriver.querySelectorAll(selector: String): List<Int> {
    return native.querySelectorAll(selector)
}

suspend fun PulsarWebDriver.click(nodeId: Int) {
    native.click(nodeId)
}

suspend fun PulsarWebDriver.press(nodeId: Int, key: String) {
    native.press(nodeId, key)
}

suspend fun PulsarWebDriver.type(nodeId: Int, text: String) {
    native.type(nodeId, text)
}

suspend fun PulsarWebDriver.fill(nodeId: Int, text: String) {
    native.fill(nodeId, text)
}
