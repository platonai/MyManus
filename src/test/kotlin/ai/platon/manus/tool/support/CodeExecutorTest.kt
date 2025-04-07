package ai.platon.manus.tool.support

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS

class CodeExecutorTest {
    @Test
    fun testExecuteJavaCode() {
        val code = """
            import java.util.Arrays;
            import java.util.List;
            
            public class Test {
                public static void main(String[] args) {
                    List<String> list = Arrays.asList("a", "b", "c");
                    System.out.println(list);
                }
            }
        """.trimIndent()

        val result = CodeExecutor.execute(code, "java")
        println(result)
    }

    @Test
    fun testExecutePythonCode() {
        val code = """
            import os
            
            def main():
                print("Hello, World!")
            
            if __name__ == "__main__":
                main()
        """.trimIndent()

        val result = CodeExecutor.execute(code, "python")
        println(result)
    }

    @Test
    @EnabledOnOs(value = [OS.LINUX, OS.MAC])
    fun testExecuteBashCode() {
        val code = """
            #!/bin/bash
            
            echo "Hello, World!"
        """.trimIndent()

        val result = CodeExecutor.execute(code, "bash")
        println(result)
    }
}
