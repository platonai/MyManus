package ai.platon.manus.common

import java.nio.file.Paths

object MyContext {

    val TARGET_DIR: String get() = Paths.get(System.getProperty("user.dir"), "target").toString()
    val AGENT_WORKING_DIR: String get() = TARGET_DIR

}
