/**
 * Functions that handle clipboard operations.
 */
export {
    copyToClipboard
}

/**
 * Copy the data in the "data-clipboard-text" attribute into the user's
 * clipboard, when a button with that element as target is clicked.
 * @param {DOMElement} element 
 */
function copyToClipboard(element) {
    let el = $(element.target)
	el = el.is("button")?el:el.parent("button")
	let text = el.attr("data-clipboard-text")

    let $temp = $("<input>")
    $("body").append($temp)
    $temp.val(text).select()
    document.execCommand("copy")
    $temp.remove()
}