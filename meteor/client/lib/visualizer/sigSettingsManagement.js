sigSettings = (function sigSettings() {
    let nodeLabels = []
    let nodeColors = [{ type: 'univ', color: '#2ECC40' }]
    let nodeShapes = [{ type: 'univ', shape: 'ellipse' }]
    let nodeBorders = [{ type: 'univ', border: 'solid' }]
    let nodeVisibility = [{ type: 'univ', visibility: false }, { type: 'Int', visibility: true }]

    /**
     * Initialize signature settings structures.
     */
    function init(settings) {
        nodeLabels = settings.nodeLabels || []
        nodeColors = settings.nodeColors || [{ type: 'univ', color: '#2ECC40' }]
        nodeShapes = settings.nodeShapes || [{ type: 'univ', shape: 'ellipse' }]
        nodeBorders = settings.nodeBorders || [{ type: 'univ', border: 'solid' }]
        nodeVisibility = settings.nodeVisibility || [{ type: 'univ', visibility: false }, { type: 'Int', visibility: true }]
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

    return {
        init,
        data,
        getAtomVisibility,
        getAtomBorder,
        getAtomShape,
        getAtomColor,
        getAtomLabel,
        getInheritedAtomVisibility,
        getInheritedAtomBorder,
        getInheritedAtomShape,
        getInheritedAtomColor,
        updateAtomVisibility,
        updateAtomBorder,
        updateAtomShape,
        updateAtomColor
    }
}())
