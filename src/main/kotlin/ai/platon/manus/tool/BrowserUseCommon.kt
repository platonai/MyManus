package ai.platon.manus.tool

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
const val PARAM_VI = "vi"
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

const val BROWSER_USE_TOOL_DESCRIPTION = """
Interact with the web browser to perform various actions such as navigation, element interaction, content extraction, and tab management.  
For search-related tasks, prioritize using this tool.

### Supported Actions:

- `'navigate'`: Visit a specific URL (defaults to `https://baidu.com`)
- `'click'`: Click an element by its index
- `'input_text'`: Enter text into an element; for Baidu, the input box has a specific index
- `'key_enter'`: Press the Enter key
- `'screenshot'`: Capture a screenshot of the screen
- `'get_html'`: Retrieve the HTML content of the current page (URL parameters not supported)
- `'get_text'`: Retrieve the text content of the current page (URL parameters not supported)
- `'execute_js'`: Execute JavaScript code
- `'scroll'`: Scroll the page
- `'switch_tab'`: Switch to a specific tab
- `'new_tab'`: Open a new tab
- `'close_tab'`: Close the current tab
- `'refresh'`: Refresh the current page

"""

const val BROWSER_USE_TOOL_PARAMETERS = """
{
    "type": "object",
    "properties": {
        "action": {
            "type": "string",
            "enum": [
                "navigate",
                "click",
                "input_text",
                "key_enter",
                "screenshot",
                "get_html",
                "get_text",
                "execute_js",
                "scroll",
                "switch_tab",
                "new_tab",
                "close_tab",
                "refresh"
            ],
            "description": "The browser action to perform"
        },
        "url": {
            "type": "string",
            "description": "URL for 'navigate' or 'new_tab' actions , don't support get_text and get_html"
        },
        "index": {
            "type": "integer",
            "description": "Element index for 'click' or 'input_text' actions"
        },
        "text": {
            "type": "string",
            "description": "Text for 'input_text' action"
        },
        "script": {
            "type": "string",
            "description": "JavaScript code for 'execute_js' action"
        },
        "scroll_amount": {
            "type": "integer",
            "description": "Pixels to scroll (positive for down, negative for up) for 'scroll' action"
        },
        "tab_id": {
            "type": "integer",
            "description": "Tab ID for 'switch_tab' action"
        }
    },
    "required": [
        "action"
    ],
    "dependencies": {
        "navigate": [
            "url"
        ],
        "click": [
            "index"
        ],
        "input_text": [
            "index",
            "text"
        ],
        "key_enter": [
            "index"
        ],
        "execute_js": [
            "script"
        ],
        "switch_tab": [
            "tab_id"
        ],
        "new_tab": [
            "url"
        ],
        "scroll": [
            "scroll_amount"
        ]
    }
}
"""
