// This script is injected into the page

function getInteractiveElements() {
    const elements = document.querySelectorAll(`
            a, button, input, select, textarea, 
            [role='button'], [role='link'],
            [onclick], [onmousedown], [onmouseup]
        `);
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

/**
 * Collects text content from all text nodes in the DOM tree
 * @param {Node} node - The root node to start traversal from (defaults to document.body)
 * @returns {string} The concatenated text content from all text nodes
 */
function getAllTextContent(node = document.body) {
    let text = '';

    // Create a TreeWalker to traverse all nodes
    const walker = document.createTreeWalker(
        node,
        NodeFilter.SHOW_TEXT,
        null
    );

    // Traverse all text nodes
    let currentNode;
    while (currentNode = walker.nextNode()) {
        // Trim the text and add it to our result
        const cleanText = currentNode.textContent
            ?.replace(/[\x00-\x1F\x7F-\x9F]/g, ' ') // remove control characters
            ?.replace(/\s+/g, " ")
        if (cleanText) {
            text += cleanText + ' ';
        }
    }

    return text.trim();
}
