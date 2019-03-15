var relationSettings = {}
relationSettings.edgeLabels = []
relationSettings.edgeColors = []
relationSettings.edgeStyles = []
relationSettings.showAsArcs = []
relationSettings.showAsAttributes = []

/**
 * Retrieves the edge label property of a relation, initializing to the
 * relation label if undefined.
 *
 * @param {String} rel the relation for which to get the property
 * @returns {String} the value assigned to the property
 */
getEdgeLabel = function (rel) {
    for (let i = 0; i < relationSettings.edgeLabels.length; i++) {
        if (relationSettings.edgeLabels[i].relation == rel) return relationSettings.edgeLabels[i].label
    }
    relationSettings.edgeLabels.push({ relation: rel, label: rel })
    return rel
}

/**
 * Updates the edge label property of a relation. Assumes already initialized.
 *
 * @param {String} rel the relation for which to update the property
 * @param {String} newVal the new value for the property
 */
updateEdgeLabel = function (rel, newVal) {
    for (let i = 0; i < relationSettings.edgeLabels.length; i++) {
        if (relationSettings.edgeLabels[i].relation == rel) {
            relationSettings.edgeLabels[i].label = newVal
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
getEdgeColor = function (rel) {
    for (let i = 0; i < relationSettings.edgeColors.length; i++) {
        if (relationSettings.edgeColors[i].relation == rel) return relationSettings.edgeColors[i].color
    }
    relationSettings.edgeColors.push({ relation: rel, color: '#0074D9' })
    return '#0074D9'
}

/**
 * Updates the edge color property of a relation. Assumes already initialized.
 *
 * @param {String} rel the relation for which to update the property
 * @param {String} newVal the new value for the property
 */
updateEdgeColor = function (rel, newVal) {
    for (let i = 0; i < relationSettings.edgeColors.length; i++) {
        if (relationSettings.edgeColors[i].relation == rel) {
            relationSettings.edgeColors[i].color = newVal
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
getEdgeStyle = function (rel) {
    for (let i = 0; i < relationSettings.edgeStyles.length; i++) {
        if (relationSettings.edgeStyles[i].relation == rel) return relationSettings.edgeStyles[i].edgeStyle
    }
    relationSettings.edgeStyles.push({ relation: rel, edgeStyle: 'solid' })
    return 'solid'
}

/**
 * Updates the edge style property of a relation. Assumes already initialized.
 *
 * @param {String} rel the relation for which to update the property
 * @param {String} newVal the new value for the property
 */
updateEdgeStyle = function (rel, newVal) {
    for (let i = 0; i < relationSettings.edgeStyles.length; i++) {
        if (relationSettings.edgeStyles[i].relation == rel) {
            relationSettings.edgeStyles[i].edgeStyle = newVal
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
isShowAsArcsOn = function (rel) {
    for (let i = 0; i < relationSettings.showAsArcs.length; i++) {
        if (relationSettings.showAsArcs[i].relation == rel) {
            return relationSettings.showAsArcs[i].showAsArcs
        }
    }
    relationSettings.showAsArcs.push({ relation: rel, showAsArcs: true })
    return true
}

/**
 * Updates the show as arcs property of a relation. Assumes already initialized.
 *
 * @param {String} rel the relation for which to update the property
 * @param {String} newVal the new value for the property
 */
updateShowAsArcs = function (rel, newVal) {
    for (let i = 0; i < relationSettings.showAsArcs.length; i++) {
        if (relationSettings.showAsArcs[i].relation == rel) {
            relationSettings.showAsArcs[i].showAsArcs = newVal
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
isShowAsAttributesOn = function (rel) {
    for (let i = 0; i < relationSettings.showAsAttributes.length; i++) {
        if (relationSettings.showAsAttributes[i].relation == rel) {
            return relationSettings.showAsAttributes[i].showAsAttributes
        }
    }
    relationSettings.showAsAttributes.push({ relation: rel, showAsAttributes: false })
    return false
}

/**
 * Updates the show as attributes property of a relation. Assumes already
 * initialized.
 *
 * @param {String} rel the relation for which to update the property
 * @param {String} newVal the new value for the property
 */
updateShowAsAttributes = function (rel, newVal) {
    for (let i = 0; i < relationSettings.showAsAttributes.length; i++) {
        if (relationSettings.showAsAttributes[i].relation == rel) {
            relationSettings.showAsAttributes[i].showAsAttributes = newVal
            return
        }
    }
}

/**
 * Propagates the show as attribues relations into the nodes attribute field.
 */
propagateAttributes = function () {
    relationSettings.showAsAttributes.forEach((item) => {
        rel = item.relation
        val = item.showAsAttributes
        const edges = cy.edges(`[relation='${rel}']`)
        if (val) {
            const aux = {}
            for (let i = 0; i < edges.length; i++) {
                if (!aux[edges[i].source().data().id])aux[edges[i].source().data().id] = []
                aux[edges[i].source().data().id].push(edges[i].data().labelExt == ''
                    ? edges[i].target().data().label
                    : `${edges[i].data().updatedLabelExt}->${edges[i].target().data().label}${edges[i].target().data().number}`)
            }
            for (const key in aux) {
                if (!cy.nodes(`[id='${key}']`)[0].data().attributes)cy.nodes(`[id='${key}']`)[0].data().attributes = []
                cy.nodes(`[id='${key}']`)[0].data().attributes[rel] = aux[key]
            }
        } else {
            for (var i = 0; i < edges.length; i++) {
                let as = cy.nodes(`[id='${edges[i].source().data().id}']`)[0].data().attributes
                if (as) delete as[rel]
            }
        }
    })
}
