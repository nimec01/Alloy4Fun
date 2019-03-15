atomSettings = {}
atomSettings.nodeLabels = []
atomSettings.nodeColors = [{ type: 'univ', color: '#2ECC40' }]
atomSettings.nodeShapes = [{ type: 'univ', shape: 'ellipse' }]
atomSettings.nodeBorders = [{ type: 'univ', border: 'solid' }]
atomSettings.unconnectedNodes = [{ type: 'univ', unconnectedNodes: false }]
atomSettings.displayNodesNumber = [{ type: 'univ', displayNodesNumber: true }]
atomSettings.nodeVisibility = [{ type: 'univ', visibility: false }]

/**
 * Retrieves the atom label property of a sig, initializing to the sig label if
 * undefined.
 *
 * @param {String} sig the sig for which to get the property
 * @returns {String} the value assigned to the property
 */
getAtomLabel = function (sig) {
    for (let i = 0; i < atomSettings.nodeLabels.length; i++) {
        if (atomSettings.nodeLabels[i].type === sig) {
            return atomSettings.nodeLabels[i].label
        }
    }
    atomSettings.nodeLabels.push({ type: sig, label: sig })
    return sig
}

/**
 * Updates the atom label property of a sig. Assumes already initialized.
 *
 * @param {String} sig the sig for which to update the property
 * @param {String} newVal the new value for the property
 */
updateAtomLabel = function (sig, newVal) {
    for (let i = 0; i < atomSettings.nodeLabels.length; i++) {
        if (atomSettings.nodeLabels[i].type === sig) {
            atomSettings.nodeLabels[i].label = newVal
            return
        }
    }
}

/**
 * Retrieves the atom colour property of a sig, initializing it to inherit if
 * undefined.
 *
 * @param {String} sig the sig for which to get the property
 * @returns {String} the value assigned to the property
 */
getAtomColor = function (sig) {
    for (let i = 0; i < atomSettings.nodeColors.length; i++) {
        if (atomSettings.nodeColors[i].type === sig) {
            return atomSettings.nodeColors[i].color
        }
    }
    atomSettings.nodeColors.push({ type: sig, color: 'inherit' })
    return 'inherit'
}

/**
 * Recursively gets the inherited atom colour property of a sig.
 *
 * @param {String} sig the signature for which to get the property
 * @returns {String} the inherited property
 */
getInheritedAtomColor = function (sig) {
    let color = getAtomColor(sig)
    while (color === 'inherit') {
        const parent = getSigParent(sig)
        color = getAtomColor(parent)
        sig = parent
    }
    return color
}

/**
 * Updates the atom color property of a sig. Assumes already initialized.
 *
 * @param {String} sig the sig for which to update the property
 * @param {String} newVal the new value for the property
 */
updateAtomColor = function (sig, newVal) {
    for (let i = 0; i < atomSettings.nodeColors.length; i++) {
        if (atomSettings.nodeColors[i].type === sig) {
            atomSettings.nodeColors[i].color = newVal
            return
        }
    }
}

/**
 * Retrieves the atom shape property of a sig, initializing it to inherit if
 * undefined.
 *
 * @param {String} sig the sig for which to get the property
 * @returns {String} the value assigned to the property
 */
getAtomShape = function (sig) {
    for (let i = 0; i < atomSettings.nodeShapes.length; i++) {
        if (atomSettings.nodeShapes[i].type === sig) {
            return atomSettings.nodeShapes[i].shape
        }
    }
    atomSettings.nodeShapes.push({ type: sig, shape: 'inherit' })
    return 'inherit'
}

/**
 * Recursively gets the inherited atom shape property of a sig.
 *
 * @param {String} sig the signature for which to get the property
 * @returns {String} the inherited property
 */
getInheritedAtomShape = function (sig) {
    let shape = getAtomShape(sig)
    while (shape === 'inherit') {
        const parent = getSigParent(sig)
        shape = getAtomShape(parent)
        sig = parent
    }
    return shape
}

/**
 * Updates the atom shape property of a sig. Assumes already initialized.
 *
 * @param {String} sig the sig for which to update the property
 * @param {String} newVal the new value for the property
 */
updateAtomShape = function (sig, newVal) {
    for (let i = 0; i < atomSettings.nodeShapes.length; i++) {
        if (atomSettings.nodeShapes[i].type === sig) {
            atomSettings.nodeShapes[i].shape = newVal
            return
        }
    }
}

/**
 * Retrieves the atom border property of a sig, initializing it to inherit if
 * undefined.
 *
 * @param {String} sig the sig for which to get the property
 * @returns {String} the value assigned to the property
 */
getAtomBorder = function (sig) {
    for (let i = 0; i < atomSettings.nodeBorders.length; i++) {
        if (atomSettings.nodeBorders[i].type === sig) {
            return atomSettings.nodeBorders[i].border
        }
    }
    atomSettings.nodeBorders.push({ type: sig, border: 'inherit' })
    return 'inherit'
}

/**
 * Recursively gets the inherited atom border property of a sig.
 *
 * @param {String} sig the signature for which to get the property
 * @returns {String} the inherited property
 */
getInheritedAtomBorder = function (sig) {
    let border = getAtomBorder(sig)
    while (border === 'inherit') {
        const parent = getSigParent(sig)
        border = getAtomBorder(parent)
        sig = parent
    }
    return border
}

/**
 * Updates the atom border property of a sig. Assumes already initialized.
 *
 * @param {String} sig the sig for which to update the property
 * @param {String} newVal the new value for the property
 */
updateAtomBorder = function (sig, newVal) {
    for (let i = 0; i < atomSettings.nodeBorders.length; i++) {
        if (atomSettings.nodeBorders[i].type === sig) {
            atomSettings.nodeBorders[i].border = newVal
            return
        }
    }
}

/**
 * Retrieves the atom visibility property of a sig, initializing it to inherit if
 * undefined.
 *
 * @param {String} sig the sig for which to get the property
 * @returns {String} the value assigned to the property
 */
getAtomVisibility = function (sig) {
    for (let i = 0; i < atomSettings.nodeVisibility.length; i++) {
        if (atomSettings.nodeVisibility[i].type === sig) {
            return atomSettings.nodeVisibility[i].visibility
        }
    }
    atomSettings.nodeVisibility.push({ type: sig, visibility: 'inherit' })
    return 'inherit'
}

/**
 * Recursively gets the inherited atom visibility property of a sig.
 *
 * @param {String} sig the signature for which to get the property
 * @returns {String} the inherited property
 */
getInheritedAtomVisibility = function (sig) {
    let visibility = getAtomVisibility(sig)
    while (visibility === 'inherit') {
        const parent = getSigParent(sig)
        visibility = getAtomVisibility(sig)
        sig = parent
    }
    return visibility
}

/**
 * Updates the atom visibility property of a sig. Assumes already initialized.
 *
 * @param {String} sig the sig for which to update the property
 * @param {String} newVal the new value for the property
 */
updateAtomVisibility = function (sig, newVal) {
    for (let i = 0; i < atomSettings.nodeVisibility.length; i++) {
        if (atomSettings.nodeVisibility[i].type === sig) {
            atomSettings.nodeVisibility[i].visibility = newVal
            return
        }
    }
}

/**
 * Retrieves the hide unconnected nodes property of a sig, initializing it to
 * inherit if undefined.
 *
 * @param {String} sig the sig for which to get the property
 * @returns {String} the value assigned to the property
 */
getHideUnconnectedNodes = function (sig) {
    for (let i = 0; i < atomSettings.unconnectedNodes.length; i++) {
        if (atomSettings.unconnectedNodes[i].type === sig) {
            return atomSettings.unconnectedNodes[i].unconnectedNodes
        }
    }
    atomSettings.unconnectedNodes.push({ type: sig, unconnectedNodes: 'inherit' })
    return 'inherit'
}

/**
 * Recursively gets the inherited hide unconnected nodes property of a sig.
 *
 * @param {String} sig the signature for which to get the property
 * @returns {String} the inherited property
 */
getInheritedHideUnconnectedNodes = function (sig) {
    let hideUnconnectedNodes = getHideUnconnectedNodes(sig)
    while (hideUnconnectedNodes === 'inherit') {
        const parent = getSigParent(sig)
        hideUnconnectedNodes = getHideUnconnectedNodes(sig)
        sig = parent
    }

    return hideUnconnectedNodes
}

/**
 * Updates the hide unconnected nodes property of a sig. Assumes already
 * initialized.
 *
 * @param {String} sig the sig for which to update the property
 * @param {String} newVal the new value for the property
 */
updateHideUnconnectedNodes = function (sig, newVal) {
    for (let i = 0; i < atomSettings.unconnectedNodes.length; i++) {
        if (atomSettings.unconnectedNodes[i].type === sig) {
            atomSettings.unconnectedNodes[i].unconnectedNodes = newVal
            return
        }
    }
}

/**
 * Retrieves the display node number property of a sig, initializing it to
 * inherit if undefined.
 *
 * @param {String} sig the sig for which to get the property
 * @returns {String} the value assigned to the property
 */
getDisplayNodesNumber = function (sig) {
    for (let i = 0; i < atomSettings.displayNodesNumber.length; i++) {
        if (atomSettings.displayNodesNumber[i].type === sig) {
            return atomSettings.displayNodesNumber[i].displayNodesNumber
        }
    }
    atomSettings.displayNodesNumber.push({ type: sig, displayNodesNumber: 'inherit' })
    return 'inherit'
}

/**
 * Recursively gets the inherited display node numbers property of a sig.
 *
 * @param {String} sig the signature for which to get the property
 * @returns {String} the inherited property
 */
getInheritedDisplayNodesNumber = function (sig) {
    let displayNodesNumber = getDisplayNodesNumber(sig)
    while (displayNodesNumber === 'inherit') {
        const parent = getSigParent(sig)
        displayNodesNumber = getDisplayNodesNumber(sig)
        sig = parent
    }
    return displayNodesNumber
}

/**
 * Updates the display node numbers property of a sig. Assumes already
 * initialized.
 *
 * @param {String} sig the sig for which to update the property
 * @param {String} newVal the new value for the property
 */
updateDisplayNodesNumber = function (sig, newVal) {
    for (let i = 0; i < atomSettings.displayNodesNumber.length; i++) {
        if (atomSettings.displayNodesNumber[i].type === sig) {
            atomSettings.displayNodesNumber[i].displayNodesNumber = newVal
            return
        }
    }
}
