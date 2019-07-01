relationSettings = (function relationSettings() {
    let edgeLabels = []
    let edgeColors = []
    let edgeStyles = []
    let showAsArcs = []
    let showAsAttributes = []

    /**
     * Initialize relation settings structures.
     */
    function init(settings) {
        edgeLabels = settings.edgeLabels || []
        edgeColors = settings.edgeColors || []
        edgeStyles = settings.edgeStyles || []
        showAsArcs = settings.showAsArcs || []
        showAsAttributes = settings.showAsAttributes || []
    }

    /**
     * Export relation settings structures as object.
     */
    function data() {
        const data = { edgeLabels,
            edgeColors,
            edgeStyles,
            showAsAttributes,
            showAsAttributes,
            showAsArcs }
        return data
    }

    /**
     * Retrieves the edge label property of a relation, initializing to the
     * relation label if undefined.
     *
     * @param {String} rel the relation for which to get the property
     * @returns {String} the value assigned to the property
     */
    function getEdgeLabel(rel) {
        for (let i = 0; i < edgeLabels.length; i++) {
            if (edgeLabels[i].relation === rel) {
                return edgeLabels[i].label
            }
        }
        edgeLabels.push({ relation: rel, label: rel })
        return rel
    }

    /**
     * Updates the edge label property of a relation. Assumes already initialized.
     *
     * @param {String} rel the relation for which to update the property
     * @param {String} newVal the new value for the property
     */
    function updateEdgeLabel(rel, newVal) {
        for (let i = 0; i < edgeLabels.length; i++) {
            if (edgeLabels[i].relation === rel) {
                edgeLabels[i].label = newVal
                return
            }
        }
    }

    /**
     * Retrieves the edge color property of a relation, initializing to a default
     * color if undefined.
     *
     * @param {String} rel the relation for which to get the property
     * @returns {String} the value assigned to the property
     */
    function getEdgeColor(rel) {
        for (let i = 0; i < edgeColors.length; i++) {
            if (edgeColors[i].relation === rel) {
                return edgeColors[i].color
            }
        }
        edgeColors.push({ relation: rel, color: '#0074D9' })
        return '#0074D9'
    }

    /**
     * Updates the edge color property of a relation. Assumes already initialized.
     *
     * @param {String} rel the relation for which to update the property
     * @param {String} newVal the new value for the property
     */
    function updateEdgeColor(rel, newVal) {
        for (let i = 0; i < edgeColors.length; i++) {
            if (edgeColors[i].relation === rel) {
                edgeColors[i].color = newVal
                return
            }
        }
    }

    /**
     * Retrieves the edge style property of a relation, initializing to solid if
     * undefined.
     *
     * @param {String} rel the relation for which to get the property
     * @returns {String} the value assigned to the property
     */
    function getEdgeStyle(rel) {
        for (let i = 0; i < edgeStyles.length; i++) {
            if (edgeStyles[i].relation === rel) {
                return edgeStyles[i].edgeStyle
            }
        }
        edgeStyles.push({ relation: rel, edgeStyle: 'solid' })
        return 'solid'
    }

    /**
     * Updates the edge style property of a relation. Assumes already initialized.
     *
     * @param {String} rel the relation for which to update the property
     * @param {String} newVal the new value for the property
     */
    function updateEdgeStyle(rel, newVal) {
        for (let i = 0; i < edgeStyles.length; i++) {
            if (edgeStyles[i].relation === rel) {
                edgeStyles[i].edgeStyle = newVal
                return
            }
        }
    }

    /**
     * Retrieves the show as arcs property of a relation, initializing to
     * true if undefined.
     *
     * @param {String} rel the relation for which to get the property
     * @returns {String} the value assigned to the property
     */
    function isShowAsArcsOn(rel) {
        for (let i = 0; i < showAsArcs.length; i++) {
            if (showAsArcs[i].relation === rel) {
                return showAsArcs[i].showAsArcs
            }
        }
        showAsArcs.push({ relation: rel, showAsArcs: true })
        return true
    }

    /**
     * Updates the show as arcs property of a relation. Assumes already initialized.
     *
     * @param {String} rel the relation for which to update the property
     * @param {String} newVal the new value for the property
     */
    function updateShowAsArcs(rel, newVal) {
        for (let i = 0; i < showAsArcs.length; i++) {
            if (showAsArcs[i].relation === rel) {
                showAsArcs[i].showAsArcs = newVal
                return
            }
        }
    }

    /**
     * Retrieves the show as attributes property of a relation, initializing to
     * false if undefined.
     *
     * @param {String} rel the relation for which to get the property
     * @returns {String} the value assigned to the property
     */
    function isShowAsAttributesOn(rel) {
        for (let i = 0; i < showAsAttributes.length; i++) {
            if (showAsAttributes[i].relation === rel) {
                return showAsAttributes[i].showAsAttributes
            }
        }
        showAsAttributes.push({ relation: rel, showAsAttributes: false })
        return false
    }

    /**
     * Updates the show as attributes property of a relation. Assumes already
     * initialized.
     *
     * @param {String} rel the relation for which to update the property
     * @param {String} newVal the new value for the property
     */
    function updateShowAsAttributes(rel, newVal) {
        for (let i = 0; i < showAsAttributes.length; i++) {
            if (showAsAttributes[i].relation === rel) {
                showAsAttributes[i].showAsAttributes = newVal
                return
            }
        }
    }

    /**
     * Propagates the show as attribues relations into the nodes attribute field.
     */
    function propagateAttributes() {
        showAsAttributes.forEach((item) => {
            rel = item.relation
            val = item.showAsAttributes
            cy.nodes().forEach((n) => {
                n.data().attributes = []
            })
            const edges = cy.edges(`[relation='${rel}']`)
            if (val) {
                const aux = {}
                for (let i = 0; i < edges.length; i++) {
                    if (!aux[edges[i].source().data().id]) aux[edges[i].source().data().id] = []
                    console.log(`${edges[i].data().updatedLabelExt}->${edges[i].target().data().label}${edges[i].target().data().number}`)
                    aux[edges[i].source().data().id].push(edges[i].data().labelExt === ''
                        ? edges[i].target().data().label+'$'+edges[i].target().data().number
                        : `${edges[i].data().updatedLabelExt}->${edges[i].target().data().label}${edges[i].target().data().number}`)
                }
                for (const key in aux) {
                    if (!cy.nodes(`[id='${key}']`)[0].data().attributes)cy.nodes(`[id='${key}']`)[0].data().attributes = []
                    cy.nodes(`[id='${key}']`)[0].data().attributes[rel] = aux[key]
                }
            }
        })
    }

    return {
        init,
        data,
        getEdgeStyle,
        getEdgeColor,
        getEdgeLabel,
        isShowAsAttributesOn,
        isShowAsArcsOn,
        propagateAttributes,
        updateEdgeStyle,
        updateEdgeColor,
        updateEdgeLabel,
        updateShowAsAttributes,
        updateShowAsArcs
    }
}())
