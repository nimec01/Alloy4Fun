import {
    displayError
} from "../../lib/editor/feedback"
import {
    getCommandIndex,
    storeInstances,
    modelExecuted,
    getNextInstance,
    getPreviousInstance,
    instChanged,
    isUnsatInstance
} from "../../lib/editor/state"

export {
    executeModel,
    nextInstance,
    prevInstance
}

/** 
 * Execute the model through the selected command. Will call the Alloy API.
 */
function executeModel () {
    let commandIndex = getCommandIndex();
    //no command to run
    if (commandIndex < 0) {
        swal({
            title: "",
            text: "There are no commands to execute",
            icon: "warning",
            buttons: true,
            dangerMode: true,
        });
    } 
    // execute command
    else { 
        let model = textEditor.getValue();
        Session.set('maxInstance',0); // this is needed so that the visualizer is shown by the helper before the handler is executed
        Meteor.call('getInstances', model, commandIndex, Session.get("from_private"), Session.get("last_id"), handleExecuteModel);
    }
}

/** 
 * Show the next instance for the executed command or requests additional
 * instances if no more cached, unless already unsat. May call the Alloy API.
 */
function nextInstance() {
    const instanceIndex = Session.get('currentInstance');
    const maxInstanceNumber = Session.get('maxInstance');
    // no more local instances but still not unsat
    if (instanceIndex == maxInstanceNumber-1 && !isUnsatInstance(instanceIndex)) {
        let model = textEditor.getValue();
        Meteor.call('nextInstances', model, getCommandIndex(), Session.get("last_id"), handleExecuteModel);
    }
    let ni = getNextInstance();
    if (typeof ni !== 'undefined') {
        if (ni.unsat) {
            Session.set('currentInstance',instanceIndex);
            swal("No more satisfying instances!", "", "error");
        } else {
            resetPositions();
            updateGraph(ni);
            instChanged();
        }
    }
}

/** 
 * Show the previous instance for the executed command, always cached if any.
 */
function prevInstance() {
    let ni = getPreviousInstance();
    if (typeof ni !== 'undefined') {
        resetPositions();
        updateGraph(ni);
    }
    instChanged();
}

/**
 * Handles the response of the Alloy API for the execution of a command,
 * either a fresh execution or a new batch of instances when iterating.
 */
function handleExecuteModel(err, result) {
    if (err) {
        maxInstanceNumber = -1;
        return displayError(err);
    }
    Session.set('last_id', result.newModelId) // update the last_id for next derivations

    result = result.instances;
    storeInstances(result);
    if (Array.isArray(result))
        result = result[0];
       
    // if there error returned by Alloy
    if (result.alloy_error) {
        let resmsg = result.msg
        if (result.line)
            resmsg = resmsg + " (" + result.line + ":" + result.column + ")"
        resmsg = resmsg + "\n"
        swal("There was a problem running the model!", resmsg + "Please validate your model.", "error");
    } 
    // else, show instance or unsat
    else {
        let command;
        if (Session.get('commands').length <= 1)
            command = Session.get('commands')[0]
        else
            command = $('.command-selection > select option:selected').text();

        if (result.warning_error) {
            let resmsg = result.msg
            if (result.line)
                resmsg = resmsg + " (" + result.line + ":" + result.column + ")"
            resmsg = resmsg + "\n"
            swal("There is a possible problem with the model!", resmsg, "warning");
        }
        if (result.unsat) {
            Session.set('log-message',result.check ? "No counter-examples. " + command + " may be valid." : "No instance found. " + command + " may be inconsistent.");
            Session.set('log-class',result.check ? "log-complete": "log-wrong");
        } else {
            Session.set('log-message',result.check ? "Counter-example found. " + command + " is invalid." : "Instance found. " + command + " is consistent.");
            Session.set('log-class',result.check ? "log-wrong" : "log-complete");
            initGraphViewer('instance');
            resetPositions();
            updateGraph(result);
        }
    }
    modelExecuted();
}