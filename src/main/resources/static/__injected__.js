// This script is injected into the page

function get_interactive_elements() {
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
