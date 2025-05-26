function get_interactive_elements() {
    const elements = document.querySelectorAll(`
            a, button, input, select, textarea, 
            [role='button'], [role='link'],
            [onclick], [onmousedown], [onmouseup]
        `);
    return Array.from(elements).map((el, index) => {
        const style = window.getComputedStyle(el);
        return {
            index: index,
            tagName: el.tagName.toLowerCase(),
            type: el.getAttribute('type'),
            role: el.getAttribute('role'),
            text: el.textContent?.trim()?.substring(0, 200) || '',
            value: el.value,
            placeholder: el.getAttribute('placeholder'),
            name: el.getAttribute('name'),
            id: el.getAttribute('id'),
            'aria-label': el.getAttribute('aria-label'),
            isVisible: (
                el.offsetWidth > 0 &&
                el.offsetHeight > 0 &&
                style.visibility !== 'hidden' &&
                style.display !== 'none'
            )
        };
    });
}
