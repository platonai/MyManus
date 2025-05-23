package ai.platon.manus.tool

import ai.platon.manus.browser.setContent
import ai.platon.pulsar.protocol.browser.driver.cdt.PulsarWebDriver
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BrowserUseToolTest {

    private lateinit var browserUseTool: BrowserUseTool
    private lateinit var driver: PulsarWebDriver

    @BeforeEach
    fun setUp() {
        browserUseTool = BrowserUseTool()
        driver = browserUseTool.driver
    }

    @AfterEach
    fun tearDown() {
        driver.close()
        BrowserUseTool.newDriver()
    }

    @Test
    fun testNavigateAction() {
        val toolInput = """{"action": "navigate", "url": "https://example.com"}"""
        val result = browserUseTool.invoke(toolInput)
        assertEquals("Navigated to https://example.com", result.message)
    }

    @Test
    fun testClickAction() {
        val toolInput = """{"action": "click", "index": 0}"""
        runBlocking {
            driver.setContent("<button>Click me</button>")
        }
        val result = browserUseTool.invoke(toolInput)
        assertEquals("Clicked element at #0", result.message)
    }

    @Test
    fun testInputTextAction() {
        val toolInput = """{"action": "input_text", "index": 0, "text": "Hello, World!"}"""
        runBlocking {
            driver.setContent("<input type='text' />")
        }
        val result = browserUseTool.invoke(toolInput)
        assertEquals("Successfully input 'Hello, World!' into element at #0", result.message)
    }

    @Test
    fun testKeyEnterAction() {
        val toolInput = """{"action": "key_enter", "index": 0}"""
        runBlocking {
            driver.setContent("<input type='text' />")
        }
        val result = browserUseTool.invoke(toolInput)
        assertEquals("Hit the enter key at #0", result.message)
    }

    @Test
    fun testScreenshotAction() {
        val toolInput = """{"action": "screenshot"}"""
        val result = browserUseTool.invoke(toolInput)
        assertTrue(result.message.toString().contains("Screenshot captured"))
    }

    @Test
    fun testGetHtmlAction() {
        val toolInput = """{"action": "get_html"}"""

        runBlocking {
            driver.setContent("<html><body>Hello, World!</body></html>")
        }

        val result = browserUseTool.invoke(toolInput)
        assertTrue(result.message.contains("Hello, World!"))
    }

    @Test
    fun testGetTextAction() {
        val toolInput = """{"action": "get_text"}"""

        runBlocking {
            driver.setContent("<body>Hello, World!</body>")
        }

        val result = browserUseTool.invoke(toolInput)
        assertEquals("Hello, World!", result.message)
    }

    @Test
    fun testExecuteJsAction() {
        val toolInput = """{"action": "execute_js", "script": "1 + 1;"}"""
        val result = browserUseTool.invoke(toolInput)
        assertEquals("2", result.message)
    }

    @Test
    fun testScrollAction() {
        val toolInput = """{"action": "scroll", "scroll_amount": 100}"""
        val result = browserUseTool.invoke(toolInput)
        assertEquals("Scrolled down by 100.0 pixels", result.message)
    }

    @Test
    fun testNewTabAction() {
        val toolInput = """{"action": "new_tab", "url": "https://example.com"}"""
        val result = browserUseTool.invoke(toolInput)
        assertEquals("Opened new tab | https://example.com", result.message)
    }

    @Test
    fun testCloseTabAction() {
        runBlocking { driver.navigateTo("https://www.example.com") }
        val toolInput = """{"action": "close_tab"}"""
        val result = browserUseTool.invoke(toolInput)
        assertEquals("Closed current tab", result.message)
    }

    @Test
    fun testSwitchTabAction() {
        runBlocking { driver.navigateTo("https://www.example.com/1") }
        runBlocking { driver.navigateTo("https://www.example.com/2") }

        val toolInput = """{"action": "switch_tab", "tab_id": 0}"""
        val result = browserUseTool.invoke(toolInput)
        assertEquals("Switched to tab | 0", result.message)
    }

    @Test
    fun testRefreshAction() {
        val toolInput = """{"action": "refresh"}"""
        val result = browserUseTool.invoke(toolInput)
        assertEquals("Page refreshed", result.message)
    }
}
