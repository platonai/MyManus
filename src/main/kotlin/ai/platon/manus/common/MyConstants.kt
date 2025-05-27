package ai.platon.manus.common

import ai.platon.manus.tool.SCROLL_PIXELS_ABOVE
import ai.platon.manus.tool.SCROLL_PIXELS_BELOW
import ai.platon.manus.tool.SCROLL_TOTAL_HEIGHT
import ai.platon.manus.tool.SCROLL_VIEWPORT_HEIGHT

// Prompt template placeholders
const val PLACEHOLDER_URL = "url_placeholder"
const val PLACEHOLDER_TABS = "tabs_placeholder"
const val PLACEHOLDER_CONTENT_ABOVE = "content_above_placeholder"
const val PLACEHOLDER_CONTENT_BELOW = "content_below_placeholder"
const val PLACEHOLDER_INTERACTIVE_ELEMENTS = "interactive_elements"
const val PLACEHOLDER_RESULTS = "results_placeholder"

// Browser use tool
const val BROWSER_INTERACTIVE_ELEMENTS_SELECTOR = "a, button, input, select, textarea, " +
        "[role='button'], [role='link'], [onclick], [onmousedown], [onmouseup]"

const val JS_GET_SCROLL_INFO = """
() => {
    return {
        $SCROLL_PIXELS_ABOVE: window.pageYOffset,
        $SCROLL_PIXELS_BELOW: Math.max(0, document.documentElement.scrollHeight - (window.pageYOffset + window.innerHeight)),
        $SCROLL_TOTAL_HEIGHT: document.documentElement.scrollHeight,
        $SCROLL_VIEWPORT_HEIGHT: window.innerHeight
    }
}
"""

const val JS_GET_INTERACTIVE_ELEMENTS = """
() => {
    const elements = document.querySelectorAll(`$BROWSER_INTERACTIVE_ELEMENTS_SELECTOR`);
    const elementsInfo = Array.from(elements).map((el, index) => {
        const style = window.getComputedStyle(el);
        const rect = el.getBoundingClientRect();
        const text = el.textContent
            ?.replace(/[\x00-\x1F\x7F-\x9F]/g, ' ') // remove control characters
            ?.replace(/\s+/g, " ")
            ?.substring(0, 200) || '';
        // replace all non-printable characters to blank space
        const value = el.value
            ?.replace(/[\x00-\x1F\x7F-\x9F]/g, ' ') // remove control characters
            ?.replace(/\s+/g, " ")
            ?.substring(0, 200)?.trim() || '';

        return {
            index: index,
            tagName: el.tagName.toLowerCase(),
            vi: el.getAttribute('vi'),
            type: el.getAttribute('type'),
            role: el.getAttribute('role'),
            text: text,
            value: value,
            placeholder: el.getAttribute('placeholder'),
            name: el.getAttribute('name'),
            id: el.getAttribute('id'),
            'aria-label': el.getAttribute('aria-label'),
            isVisible: (
                el.offsetWidth > 0 &&
                el.offsetHeight > 0 &&
                style.visibility !== 'hidden' &&
                style.display !== 'none'
            ),
            isInViewport: (
                rect.top >= 0 &&
                rect.left >= 0 &&
                rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) &&
                rect.right <= (window.innerWidth || document.documentElement.clientWidth)
            ),
        };
    });
    return elementsInfo.filter(el => el.isVisible && el.isInViewport);
}
"""

const val SERP_API_URL = "https://serpapi.com/search"
const val BROWSER_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36"
