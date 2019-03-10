export {
	modelChanged,
	cmdChanged,
	modelExecuted,
	instChanged,
	themeChanged,
	modelShared,	
	instShared,
	getCurrentInstance,
	getPreviousInstance,
	getNextInstance,
	storeInstances,
    isUnsatInstance,
    getCommandIndex
}

// Globals
/** @var instances The received instances */
var instances = [];

/**
Functions that update the internal state.
*/

function modelChanged() {
	Session.set('model-updated',true);
    Session.set('model-shared',false);
    Session.set('log-message','');
    Session.set('log-class','');
    Session.set("currentInstance", 0);
    Session.set("maxInstance", -1);
    Session.set("instances", []);
    Session.set("projectableTypes", undefined);
}

function cmdChanged() {
	Session.set('model-updated',true);
}

function modelExecuted() {
    Session.set('model-updated',false);
    instChanged();
    Session.set('from-instance',false);
}

function instChanged() {
    Session.set('inst-shared',false);
}

function themeChanged() {
    Session.set('model-shared',false);
    Session.set('inst-shared',false);
}

function modelShared() {
    Session.set('model-shared',true);
}

function instShared() {
    Session.set('inst-shared',true);	
}

function storeInstances(allInstances) {
    const instanceIndex = Session.get('currentInstance');
    const maxInstanceNumber = Session.get('maxInstance');
    if (allInstances.alloy_error || allInstances[0].cnt == 0) {
        instances = allInstances;
        Session.set('currentInstance',0);
        Session.set('maxInstance',allInstances.length);
    } else {
        instances = instances.concat(allInstances);
        Session.set('maxInstance',maxInstanceNumber + allInstances.length);
    }
}

function getNextInstance() {
    const instanceIndex = Session.get('currentInstance');
    Session.set('currentInstance',instanceIndex+1)
    return instances[instanceIndex+1];
}

function getPreviousInstance() {
    const instanceIndex = Session.get('currentInstance');
    Session.set('currentInstance',instanceIndex-1)
    return instances[instanceIndex-1];
}

function getCurrentInstance() {
    const instanceIndex = Session.get('currentInstance');
    return instances[instanceIndex]
}

function isUnsatInstance(i) {
	return instances[i].unsat
}

function getCommandIndex() {
    let i = -1;
    if (Session.get("commands").length == 1) i = 0;
    else if (Session.get("commands").length > 0) i = $('.command-selection > select option:selected').index()
    return i;
}