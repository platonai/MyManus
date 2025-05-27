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
const val PARAM_BOUNDING_BOX = "bounding_box"
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

// Element info constants
const val ELEMENT_INDEX = "index"
const val ELEMENT_TAG_NAME = "tagName"
const val ELEMENT_TYPE = "type"
const val ELEMENT_ROLE = "role"
const val ELEMENT_TEXT = "text"
const val ELEMENT_VALUE = "value"
const val ELEMENT_PLACEHOLDER = "placeholder"
const val ELEMENT_NAME = "name"
const val ELEMENT_ID = "id"
const val ELEMENT_ARIA_LABEL = "aria-label"
const val ELEMENT_IS_VISIBLE = "isVisible"

const val BROWSER_USE_TOOL_DESCRIPTION = """
Automate web browser interactions including visiting pages, clicking elements, extracting content, and managing tabs.

You can perform these core actions:

- '$ACTION_NAVIGATE': Go to a specific URL
- '$ACTION_CLICK': Click an element by index
- '$ACTION_INPUT_TEXT': Input text into an element
- '$ACTION_KEY_ENTER': Hit the Enter key
- '$ACTION_SCREENSHOT': Capture a screenshot
- '$ACTION_GET_HTML': Get page HTML content
- '$ACTION_GET_TEXT': Get text content of the page
- '$ACTION_EXECUTE_JS': Execute JavaScript code
- '$ACTION_SCROLL': Scroll the page
- '$ACTION_SWITCH_TAB': Switch to a specific tab
- '$ACTION_NEW_TAB': Open a new tab
- '$ACTION_CLOSE_TAB': Close the current tab
- '$ACTION_REFRESH': Refresh the current page
"""

const val BROWSER_USE_TOOL_PARAMETERS = """
{
    "type": "object",
    "properties": {
        "$PARAM_ACTION": {
            "type": "string",
            "enum": [
                "$ACTION_NAVIGATE",
                "$ACTION_CLICK",
                "$ACTION_INPUT_TEXT",
                "$ACTION_KEY_ENTER",
                "$ACTION_SCREENSHOT",
                "$ACTION_GET_HTML",
                "$ACTION_GET_TEXT",
                "$ACTION_EXECUTE_JS",
                "$ACTION_SCROLL",
                "$ACTION_SWITCH_TAB",
                "$ACTION_NEW_TAB",
                "$ACTION_CLOSE_TAB",
                "$ACTION_REFRESH"
            ],
            "description": "The browser action to perform"
        },
        "$PARAM_URL": {
            "type": "string",
            "description": "URL for '$ACTION_NAVIGATE' or '$ACTION_NEW_TAB' actions"
        },
        "$PARAM_INDEX": {
            "type": "integer",
            "description": "Element index for '$ACTION_CLICK' or '$ACTION_INPUT_TEXT' actions"
        },
        "$PARAM_BOUNDING_BOX": {
            "type": "string",
            "description": "Element bounding box used to locate the element for '$ACTION_CLICK' or '$ACTION_INPUT_TEXT' actions"
        },
        "$PARAM_TEXT": {
            "type": "string",
            "description": "Text for '$ACTION_INPUT_TEXT' action"
        },
        "$PARAM_SCRIPT": {
            "type": "string",
            "description": "JavaScript code for '$ACTION_EXECUTE_JS' action"
        },
        "$PARAM_SCROLL_AMOUNT": {
            "type": "integer",
            "description": "Pixels to scroll (positive for down, negative for up) for '$ACTION_SCROLL' action"
        },
        "$PARAM_TAB_ID": {
            "type": "integer",
            "description": "Tab ID for '$ACTION_SWITCH_TAB' action"
        }
    },
    "required": [
        "$PARAM_ACTION"
    ],
    "dependencies": {
        "$ACTION_NAVIGATE": [
            "$PARAM_URL"
        ],
        "$ACTION_CLICK": [
            "$PARAM_BOUNDING_BOX"
        ],
        "$ACTION_INPUT_TEXT": [
            "$PARAM_BOUNDING_BOX",
            "$PARAM_TEXT"
        ],
        "$ACTION_KEY_ENTER": [
            "$PARAM_BOUNDING_BOX"
        ],
        "$ACTION_EXECUTE_JS": [
            "$PARAM_SCRIPT"
        ],
        "$ACTION_SWITCH_TAB": [
            "$PARAM_TAB_ID"
        ],
        "$ACTION_NEW_TAB": [
            "$PARAM_URL"
        ],
        "$ACTION_SCROLL": [
            "$PARAM_SCROLL_AMOUNT"
        ]
    }
}
"""
