package ai.platon.manus.browser

import ai.platon.manus.common.JS_GET_INTERACTIVE_ELEMENTS
import ai.platon.pulsar.common.serialize.json.prettyPulsarObjectMapper
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import ai.platon.pulsar.protocol.browser.impl.DefaultBrowserFactory
import ai.platon.pulsar.skeleton.crawl.fetch.driver.Browser
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import kotlin.test.*

class BrowserTest {

    companion object {
        val browserFactory = DefaultBrowserFactory()
        val webDriverService = WebDriverService(browserFactory)
        lateinit var browser: Browser

        @JvmStatic
        @BeforeAll
        fun initBrowser() {
            browser = browserFactory.launchRandomTempBrowser()
            browser.newDriver()
        }

        @JvmStatic
        @AfterAll
        fun closeBrowser() {
            browser.close()
        }
    }

    @BeforeEach
    fun setUp() {
    }

    @AfterEach
    fun tearDown() {
    }

    @Test
    fun `test getConfig`() = webDriverService.runWebDriverTest("https://www.example.com", browser) { driver ->
        val code = """__pulsar_utils__.getConfig()"""

        val result = driver.evaluateDetail(code)
        println(result)
        assertNotNull(result)
        assertNull(result.value)
        assertNull(result.exception)
        assertEquals("Object", result.className)
        assertEquals("Object", result.description)
        // assertEquals(2, result)

        val result2 = driver.evaluateValueDetail(code)
        println(result2)
        assertNotNull(result2)
        assertNull(result2.exception)
        assertNull(result2.className)
        assertNull(result2.description)
        val value2 = result2.value
        assertNotNull(value2)
        // println(value2::class.qualifiedName)
        assertEquals("java.util.LinkedHashMap", value2::class.qualifiedName)
        assertTrue { value2 is Map<*, *> }
        value2 as Map<*, *>
        assertEquals(browser.settings.viewportSize.width, value2["viewPortWidth"])

        val propertyNames = value2["propertyNames"]
        assertNotNull(propertyNames)
        // println(propertyNames::class.qualifiedName)
        assertEquals("java.util.ArrayList", propertyNames::class.qualifiedName)
        assertTrue { propertyNames is List<*> }
    }

    @Test
    fun `test JS_GET_INTERACTIVE_ELEMENTS`() = webDriverService.runWebDriverTest("https://www.amazon.com/dp/B0C1H26C46", browser) { driver ->
        val code = JS_GET_INTERACTIVE_ELEMENTS

        val result = driver.evaluateValueDetail("($code)()")

        assertNotNull(result)
        assertNull(result.exception)

        val elementsInfo = result.value as List<MutableMap<String, Any?>>
        elementsInfo.forEach { element ->
            if (element["tagName"] == "textarea") {
                element["text"] = ""
                element["value"] = ""
            }
        }

        println(prettyPulsarObjectMapper().writeValueAsString(elementsInfo))

        val value = result.value
        assertNotNull(value)
    }
}
