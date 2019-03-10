import {
    zeroclipboard,
} from "../../lib/editor/clipboard"
import {
    displayError,
} from "../../lib/editor/feedback"
import {
    secretTag 
} from "../../../lib/editor/text"
import {
    modelShared,
    getCommandIndex,
    instShared,
} from "../../lib/editor/state"
export {
    generateModelURL,
    generateInstURL,
}

/**
 * Function to handle click on "Share" button
 */
function generateModelURL() {
    if ($("#genUrl > button").is(":disabled")) return

    const themeData = {
        atomSettings,
        relationSettings,
        generalSettings,
        currentFramePosition,
        currentlyProjectedTypes,
        metaPrimSigs,
        metaSubsetSigs,
    };

    let modelToShare = textEditor.getValue();
    Meteor.call('genURL', modelToShare, Session.get("last_id"), themeData, handleGenURLEvent);
}

function generateInstURL() {
    const themeData = {
        atomSettings,
        relationSettings,
        generalSettings,
        currentFramePosition,
        currentlyProjectedTypes,
        metaPrimSigs,
        metaSubsetSigs,
    };
    Meteor.call("storeInstance", Session.get("last_id"), getCommandIndex(), cy.json(), themeData, handleGenInstanceURLEvent)
}

/* genUrlbtn event handler after genUrl method */
function handleGenURLEvent(err, result) {
    if (err) return displayError(err)
    // if the URL was generated successfully, create and append a new element to the HTML containing it.
    Session.set('public-model-url',`${window.location.origin}/`+result['public']);
    Session.set('private-model-url',`${window.location.origin}/`+result['private']);
    modelShared();

    //update the value of the last model id
    Session.set("last_id", result.last_id); // this will change on every derivation
}

// geninstanceurlbtn event handler after storeInstance method
function handleGenInstanceURLEvent(err, result) {
    if (err) return displayError(err)

    Session.set('inst-url',`${window.location.origin}/`+result);

    instShared();
}