/**
 * Module with functions that handle the internal state of the app.
 *
 * @module client/lib/editor/state
 */

/** @var instances The received instances */
let instances = []

/**
 * Updates the state when the model has been changed.
 */
export function modelChanged() {
    Session.set('model-updated', true)
    Session.set('model-shared', false)
    Session.set('log-message', '')
    Session.set('log-class', '')
    Session.set('currentInstance', 0)
    Session.set('maxInstance', -1)
    Session.set('instances', [])
}

/**
 * Updates the state when the selected command has been changed.
 */
export function cmdChanged() {
    Session.set('model-updated', true)
}

/**
 * Updates the state when the model has been executed.
 */
export function modelExecuted() {
    Session.set('model-updated', false)
    instChanged()
    Session.set('from-instance', false)
}

/**
 * Updates the state when the instance has been changed.
 */
export function instChanged() {
    Session.set('inst-shared', false)
}

/**
 * Updates the state when the theme has been changed.
 */
export function themeChanged() {
    Session.set('model-shared', false)
    Session.set('inst-shared', false)
}

/**
 * Updates the state when the model has been shared.
 */
export function modelShared() {
    Session.set('model-shared', true)
}

/**
 * Updates the state when the instance has been shared.
 */
export function instShared() {
    Session.set('inst-shared', true)
}

/**
 * Updates the state when a new batch of instances has been received.
 */
export function storeInstances(allInstances) {
    const instanceIndex = Session.get('currentInstance')
    const maxInstanceNumber = Session.get('maxInstance')
    if (allInstances.alloy_error || allInstances[0].cnt == 0) {
        instances = allInstances
        Session.set('currentInstance', 0)
        Session.set('maxInstance', allInstances.length)
    } else {
        instances = instances.concat(allInstances)
        Session.set('maxInstance', maxInstanceNumber + allInstances.length)
    }
}

/**
 * Returns the current instance.
 *
 * @returns the current stored instance.
 */
export function getCurrentInstance() {
    const instanceIndex = Session.get('currentInstance')
    return instances[instanceIndex]
}

/**
 * Sets the next known instance as the current one and returns it.
 *
 * @returns the next stored instance.
 */
export function getNextInstance() {
    const instanceIndex = Session.get('currentInstance')
    Session.set('currentInstance', instanceIndex + 1)
    return instances[instanceIndex + 1]
}

/**
 * Sets the previous known instance as the current one and returns it.
 *
 * @returns the next stored instance.
 */
export function getPreviousInstance() {
    const instanceIndex = Session.get('currentInstance')
    Session.set('currentInstance', instanceIndex - 1)
    return instances[instanceIndex - 1]
}

/**
 * Whether an instance is unsat.
 *
 * @param {Number} the index of the instance to be tested
 * @returns whether the instance is unsat
 */
export function isUnsatInstance(i) {
    return instances[i].unsat
}

/**
 * The index of the currently selected command.
 *
 * @returns the index of the selected command
 */
export function getCommandIndex() {
    let i = -1
    if (Session.get('commands').length == 1) { i = 0 } else if (Session.get('commands').length > 0) { i = $('.command-selection > select option:selected').index() }
    return i
}

/**
 * The label of the currently selected command.
 *
 * @returns the label of the selected command
 */
export function getCommandLabel() {
    let command
    if (Session.get('commands').length <= 1) command = Session.get('commands')[0]
    else { command = $('.command-selection > select option:selected').text() }
    return command
}
