package ai.platon.manus.tool

import ai.platon.pulsar.common.Runtimes
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertContains

class PythonToolTest {
    private val python = PythonTool()

    @BeforeEach
    fun `ensure python available`() {
        val outputs = Runtimes.exec("python --version").joinToString()
        assertContains(outputs, "Python ")
    }

    @Test
    fun `test python execute`() {
        python.run(mapOf(
            "code" to """
                print("Hello, World!")
            """.trimIndent()
        ))
    }
}
