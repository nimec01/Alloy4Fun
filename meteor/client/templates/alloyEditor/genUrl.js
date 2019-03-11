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
    shareModel,
    shareInstance,
}

/**
 * Store and share the current model and generate the sharing URLs.
 */
function shareModel() {
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
    Meteor.call('genURL', modelToShare, Session.get("last_id"), themeData, handleShareModel);
}

/**
 * Store and share the current instance and generate the sharing URL. The
 * respective model is already stored due to the execution.
 */
function shareInstance() {
    const themeData = {
        atomSettings,
        relationSettings,
        generalSettings,
        currentFramePosition,
        currentlyProjectedTypes,
        metaPrimSigs,
        metaSubsetSigs,
    };
    Meteor.call("storeInstance", Session.get("last_id"), getCommandIndex(), cy.json(), themeData, handleShareInstance)
}

/* Handles the response to the model sharing request. */
function handleShareModel(err, result) {
    if (err) return displayError(err)

    Session.set('public-model-url',`${window.location.origin}/`+result['public']);
    Session.set('private-model-url',`${window.location.origin}/`+result['private']);

    modelShared();
}

/* Handles the response to the instance sharing request. */
function handleShareInstance(err, result) {
    if (err) return displayError(err)

    Session.set('inst-url',`${window.location.origin}/`+result);

    instShared();
}