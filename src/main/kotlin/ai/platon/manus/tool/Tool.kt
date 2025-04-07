package ai.platon.manus.tool

import ai.platon.manus.tool.support.ToolExecuteResult
import java.util.function.Function

interface Tool : Function<Any, ToolExecuteResult>, AutoCloseable
