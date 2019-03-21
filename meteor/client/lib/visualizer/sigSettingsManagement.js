sigSettings = (function sigSettings() {
    let nodeLabels = []
    let nodeColors = [{ type: 'univ', color: '#2ECC40' }]
    let nodeShapes = [{ type: 'univ', shape: 'ellipse' }]
    let nodeBorders = [{ type: 'univ', border: 'solid' }]
    let unconnectedNodes = [{ type: 'univ', unconnectedNodes: false }]
    let displayNodesNumber = [{ type: 'univ', displayNodesNumber: true }]
    let nodeVisibility = [{ type: 'univ', visibility: false }]

    /**
     * Initialize signature settings structures.
     */
    function init(settings) {
        nodeLabels = settings.nodeLabels || []
        nodeColors = settings.nodeColors || [{ type: 'univ', color: '#2ECC40' }]
        nodeShapes = settings.nodeShapes || [{ type: 'univ', shape: 'ellipse' }]
        nodeBorders = settings.nodeBorders || [{ type: 'univ', border: 'solid' }]
        unconnectedNodes = settings.unconnectedNodes || [{ type: 'univ', unconnectedNodes: false }]
        displayNodesNumber = settings.displayNodesNumber || [{ type: 'univ', displayNodesNumber: true }]
        nodeVisibility = settings.nodeVisibility || [{ type: 'univ', visibility: false }]
    }

    /**
     * Export signature settings structures as object.
     */
    function data() {
        const data = { nodeLabels,
            nodeColors,
            nodeShapes,
            nodeBorders,
            nodeBorders,
            unconnectedNodes,
            displayNodesNumber,
            nodeVisibility }
        return data
    }

    /**
     * Retrieves the atom label property of a sig, initializing to the sig label if
     * undefined.
     *
     * @param {String} sig the sig for which to get the property
     * @returns {String} the value assigned to the property
     */
    function getAtomLabel(sig) {
        for (let i = 0; i < nodeLabels.length; i++) {
            if (nodeLabels[i].type === sig) {
                return nodeLabels[i].label
            }
        }
        nodeLabels.push({ type: sig, label: sig })
        return sig
    }

    /**
     * Updates the atom label property of a sig. Assumes already initialized.
     *
     * @param {String} sig the sig for which to update the property
     * @param {String} newVal the new value for the property
     */
    function updateAtomLabel(sig, newVal) {
        for (let i = 0; i < nodeLabels.length; i++) {
            if (nodeLabels[i].type === sig) {
                nodeLabels[i].label = newVal
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
    function getAtomColor(sig) {
        for (let i = 0; i < nodeColors.length; i++) {
            if (nodeColors[i].type === sig) {
                return nodeColors[i].color
            }
        }
        nodeColors.push({ type: sig, color: 'inherit' })
        return 'inherit'
    }

    /**
     * Recursively gets the inherited atom colour property of a sig.
     *
     * @param {String} sig the signature for which to get the property
     * @returns {String} the inherited property
     */
    function getInheritedAtomColor(sig) {
        let cur = sig
        let color = getAtomColor(cur)
        while (color === 'inherit') {
            const parent = generalSettings.getSigParent(cur)
            color = getAtomColor(parent)
            cur = parent
        }
        return color
    }

    /**
     * Updates the atom color property of a sig. Assumes already initialized.
     *
     * @param {String} sig the sig for which to update the property
     * @param {String} newVal the new value for the property
     */
    function updateAtomColor(sig, newVal) {
        for (let i = 0; i < nodeColors.length; i++) {
            if (nodeColors[i].type === sig) {
                nodeColors[i].color = newVal
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
    function getAtomShape(sig) {
        for (let i = 0; i < nodeShapes.length; i++) {
            if (nodeShapes[i].type === sig) {
                return nodeShapes[i].shape
            }
        }
        nodeShapes.push({ type: sig, shape: 'inherit' })
        return 'inherit'
    }

    /**
     * Recursively gets the inherited atom shape property of a sig.
     *
     * @param {String} sig the signature for which to get the property
     * @returns {String} the inherited property
     */
    function getInheritedAtomShape(sig) {
        let cur = sig
        let shape = getAtomShape(cur)
        while (shape === 'inherit') {
            const parent = generalSettings.getSigParent(cur)
            shape = getAtomShape(parent)
            cur = parent
        }
        return shape
    }

    /**
     * Updates the atom shape property of a sig. Assumes already initialized.
     *
     * @param {String} sig the sig for which to update the property
     * @param {String} newVal the new value for the property
     */
    function updateAtomShape(sig, newVal) {
        for (let i = 0; i < nodeShapes.length; i++) {
            if (nodeShapes[i].type === sig) {
                nodeShapes[i].shape = newVal
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
    function getAtomBorder(sig) {
        for (let i = 0; i < nodeBorders.length; i++) {
            if (nodeBorders[i].type === sig) {
                return nodeBorders[i].border
            }
        }
        nodeBorders.push({ type: sig, border: 'inherit' })
        return 'inherit'
    }

    /**
     * Recursively gets the inherited atom border property of a sig.
     *
     * @param {String} sig the signature for which to get the property
     * @returns {String} the inherited property
     */
    function getInheritedAtomBorder(sig) {
        let cur = sig
        let border = getAtomBorder(cur)
        while (border === 'inherit') {
            const parent = generalSettings.getSigParent(cur)
            border = getAtomBorder(parent)
            cur = parent
        }
        return border
    }

    /**
     * Updates the atom border property of a sig. Assumes already initialized.
     *
     * @param {String} sig the sig for which to update the property
     * @param {String} newVal the new value for the property
     */
    function updateAtomBorder(sig, newVal) {
        for (let i = 0; i < nodeBorders.length; i++) {
            if (nodeBorders[i].type === sig) {
                nodeBorders[i].border = newVal
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
    function getAtomVisibility(sig) {
        for (let i = 0; i < nodeVisibility.length; i++) {
            if (nodeVisibility[i].type === sig) {
                return nodeVisibility[i].visibility
            }
        }
        nodeVisibility.push({ type: sig, visibility: 'inherit' })
        return 'inherit'
    }

    /**
     * Recursively gets the inherited atom visibility property of a sig.
     *
     * @param {String} sig the signature for which to get the property
     * @returns {String} the inherited property
     */
    function getInheritedAtomVisibility(sig) {
        let cur = sig
        let visibility = getAtomVisibility(cur)
        while (visibility === 'inherit') {
            const parent = generalSettings.getSigParent(cur)
            visibility = getAtomVisibility(parent)
            cur = parent
        }
        return visibility
    }

    /**
     * Updates the atom visibility property of a sig. Assumes already initialized.
     *
     * @param {String} sig the sig for which to update the property
     * @param {String} newVal the new value for the property
     */
    function updateAtomVisibility(sig, newVal) {
        for (let i = 0; i < nodeVisibility.length; i++) {
            if (nodeVisibility[i].type === sig) {
                nodeVisibility[i].visibility = newVal
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
    function getHideUnconnectedNodes(sig) {
        for (let i = 0; i < unconnectedNodes.length; i++) {
            if (unconnectedNodes[i].type === sig) {
                return unconnectedNodes[i].unconnectedNodes
            }
        }
        unconnectedNodes.push({ type: sig, unconnectedNodes: 'inherit' })
        return 'inherit'
    }

    /**
     * Recursively gets the inherited hide unconnected nodes property of a sig.
     *
     * @param {String} sig the signature for which to get the property
     * @returns {String} the inherited property
     */
    function getInheritedHideUnconnectedNodes(sig) {
        let cur = sig
        let hideUnconnectedNodes = getHideUnconnectedNodes(cur)
        while (hideUnconnectedNodes === 'inherit') {
            const parent = generalSettings.getSigParent(cur)
            hideUnconnectedNodes = getHideUnconnectedNodes(parent)
            cur = parent
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
    function updateHideUnconnectedNodes(sig, newVal) {
        for (let i = 0; i < unconnectedNodes.length; i++) {
            if (unconnectedNodes[i].type === sig) {
                unconnectedNodes[i].unconnectedNodes = newVal
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
    function getDisplayNodesNumber(sig) {
        for (let i = 0; i < displayNodesNumber.length; i++) {
            if (displayNodesNumber[i].type === sig) {
                return displayNodesNumber[i].displayNodesNumber
            }
        }
        displayNodesNumber.push({ type: sig, displayNodesNumber: 'inherit' })
        return 'inherit'
    }

    /**
     * Recursively gets the inherited display node numbers property of a sig.
     *
     * @param {String} sig the signature for which to get the property
     * @returns {String} the inherited property
     */
    function getInheritedDisplayNodesNumber(sig) {
        let cur = sig
        let display = getDisplayNodesNumber(cur)
        while (display === 'inherit') {
            const parent = generalSettings.getSigParent(cur)
            display = getDisplayNodesNumber(parent)
            cur = parent
        }
        return display
    }

    /**
     * Updates the display node numbers property of a sig. Assumes already
     * initialized.
     *
     * @param {String} sig the sig for which to update the property
     * @param {String} newVal the new value for the property
     */
    function updateDisplayNodesNumber(sig, newVal) {
        for (let i = 0; i < displayNodesNumber.length; i++) {
            if (displayNodesNumber[i].type === sig) {
                displayNodesNumber[i].displayNodesNumber = newVal
                return
            }
        }
    }

    return {
        init,
        data,
        getAtomVisibility,
        getAtomBorder,
        getAtomShape,
        getAtomColor,
        getAtomLabel,
        getHideUnconnectedNodes,
        getDisplayNodesNumber,
        getInheritedAtomVisibility,
        getInheritedAtomBorder,
        getInheritedAtomShape,
        getInheritedAtomColor,
        getInheritedDisplayNodesNumber,
        getInheritedHideUnconnectedNodes,
        updateAtomVisibility,
        updateAtomBorder,
        updateDisplayNodesNumber,
        updateAtomShape,
        updateAtomColor,
        updateAtomLabel,
        updateHideUnconnectedNodes
    }
}())
