import {
    zeroclipboard,
    getAnchorWithLink
} from "../../lib/editor/clipboard"
import {
    displayError
} from "../../lib/editor/feedback"
import {
    secretTag } 
from "../../../lib/editor/text"
export {
    clickGenUrl
}

/**
 * Function to handle click on "Share" button
 */
function clickGenUrl() {
    if ($("#genURL > button").is(":disabled")) return

    let modelToShare = textEditor.getValue();
    Meteor.call('genURL', modelToShare, Session.get("last_id"), handleGenURLEvent);
}

/* genUrlbtn event handler after genUrl method */
function handleGenURLEvent(err, result) {
    if (err) return displayError(err)
    // if the URL was generated successfully, create and append a new element to the HTML containing it.
    let url = getAnchorWithLink(result['public'], "public link");
    let urlPrivate = getAnchorWithLink(result['private'], "private link");

    let textcenter = document.createElement('div');
    textcenter.className = "text-center";
    textcenter.id = "permalink";
    textcenter.appendChild(url);
    if (urlPrivate) textcenter.appendChild(urlPrivate);

    $('#url-permalink').empty() //remove previous links
    document.getElementById('url-permalink').appendChild(textcenter);
    $("#genUrl > button").prop('disabled', true);
    zeroclipboard();

    //update the value of the last model id
    Session.set("last_id", result.last_id); // this will change on every derivation
}