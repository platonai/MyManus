package ai.platon.manus.tool

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BrowserUseToolTest {

    private lateinit var browserUseTool: BrowserUseTool
    private val page get() = browserUseTool.page

    @BeforeEach
    fun setUp() {
        browserUseTool = BrowserUseTool()
        browserUseTool.initializeBrowserIfNecessary()
    }

    @AfterEach
    fun tearDown() {
        browserUseTool.close()
    }

    @Test
    fun testNavigateAction() {
        val toolInput = """{"action": "navigate", "url": "https://example.com"}"""
        val result = browserUseTool.apply(toolInput)
        assertEquals("Navigated to https://example.com", result.message)
    }

    @Test
    fun testClickAction() {
        val toolInput = """{"action": "click", "index": 0}"""
        page.setContent("<button>Click me</button>")
        val result = browserUseTool.apply(toolInput)
        assertEquals("Clicked element at #0", result.message)
    }

    @Test
    fun testInputTextAction() {
        val toolInput = """{"action": "input_text", "index": 0, "text": "Hello, World!"}"""
        page.setContent("<input type='text' />")
        val result = browserUseTool.apply(toolInput)
        assertEquals("Successfully input 'Hello, World!' into element at #0", result.message)
    }

    @Test
    fun testKeyEnterAction() {
        val toolInput = """{"action": "key_enter", "index": 0}"""
        page.setContent("<input type='text' />")
        val result = browserUseTool.apply(toolInput)
        assertEquals("Hit the enter key at #0", result.message)
    }

    @Test
    fun testScreenshotAction() {
        val toolInput = """{"action": "screenshot"}"""
        val result = browserUseTool.apply(toolInput)
        assertTrue(result.message.toString().contains("Screenshot captured"))
    }

    @Test
    fun testGetHtmlAction() {
        val toolInput = """{"action": "get_html"}"""
        page.setContent("<html><body>Hello, World!</body></html>")
        val result = browserUseTool.apply(toolInput)
        assertTrue(result.message.toString().contains("Hello, World!"))
    }

    @Test
    fun testGetTextAction() {
        val toolInput = """{"action": "get_text"}"""
        page.setContent("<body>Hello, World!</body>")
        val result = browserUseTool.apply(toolInput)
        assertEquals("Hello, World!", result.message)
    }

    @Test
    fun testExecuteJsAction() {
        val toolInput = """{"action": "execute_js", "script": "1 + 1;"}"""
        val result = browserUseTool.apply(toolInput)
        assertEquals("2", result.message)
    }

    @Test
    fun testScrollAction() {
        val toolInput = """{"action": "scroll", "scroll_amount": 100}"""
        val result = browserUseTool.apply(toolInput)
        assertEquals("Scrolled down by 100.0 pixels", result.message)
    }

    @Test
    fun testNewTabAction() {
        val toolInput = """{"action": "new_tab", "url": "https://example.com"}"""
        val result = browserUseTool.apply(toolInput)
        assertEquals("Opened new tab | https://example.com", result.message)
    }

    @Test
    fun testCloseTabAction() {
        val toolInput = """{"action": "close_tab"}"""
        val result = browserUseTool.apply(toolInput)
        assertEquals("Closed current tab", result.message)
    }

    @Test
    fun testSwitchTabAction() {
        val toolInput = """{"action": "switch_tab", "tab_id": 0}"""
        val result = browserUseTool.apply(toolInput)
        assertEquals("Switched to tab | 0", result.message)
    }

    @Test
    fun testRefreshAction() {
        val toolInput = """{"action": "refresh"}"""
        val result = browserUseTool.apply(toolInput)
        assertEquals("Page refreshed", result.message)
    }
}
