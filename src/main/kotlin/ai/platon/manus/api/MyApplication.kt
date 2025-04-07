package ai.platon.manus.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MyManusApplication

fun main(args: Array<String>) {
    // active the following two profiles: interactive, private
    System.setProperty("spring.profiles.active", "interactive,private")

    runApplication<MyManusApplication>(*args)
}
