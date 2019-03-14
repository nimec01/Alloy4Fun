relationSettings = {}
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
getRelationLabel = function (rel) {
    if (relationSettings && relationSettings.edgeLabels) {
        for (let i = 0; i < relationSettings.edgeLabels.length; i++) {
            if (relationSettings.edgeLabels[i].type == rel) return relationSettings.edgeLabels[i].label
        }
    } else {
        relationSettings.edgeLabels = []
    }
    relationSettings.edgeLabels.push({ type: rel, label: rel })
    return rel
}

updateRelationLabel = function (rel, newVal) {
    for (let i = 0; i < relationSettings.edgeLabels.length; i++) {
        if (relationSettings.edgeLabels[i].type == rel) {
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
getRelationColor = function (rel) {
    if (relationSettings && relationSettings.edgeColors) {
        for (let i = 0; i < relationSettings.edgeColors.length; i++) {
            if (relationSettings.edgeColors[i].type == rel) return relationSettings.edgeColors[i].color
        }
    } else {
        relationSettings.edgeColors = []
    }
    relationSettings.edgeColors.push({ type: rel, color: '#0074D9' })
    return '#0074D9'
}

updateRelationColor = function (rel, newVal) {
    for (let i = 0; i < relationSettings.edgeColors.length; i++) {
        if (relationSettings.edgeColors[i].type == rel) {
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
getRelationEdgeStyle = function (rel) {
    console.log('called style')
    if (relationSettings && relationSettings.edgeStyles) {
        for (let i = 0; i < relationSettings.edgeStyles.length; i++) {
            if (relationSettings.edgeStyles[i].type == rel) return relationSettings.edgeStyles[i].edgeStyle
        }
    } else {
        relationSettings.edgeStyles = []
    }
    relationSettings.edgeStyles.push({ type: rel, edgeStyle: 'solid' })
    return 'solid'
}

updateEdgeStyle = function (rel, newVal) {
    for (let i = 0; i < relationSettings.edgeStyles.length; i++) {
        if (relationSettings.edgeStyles[i].type == rel) {
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
    console.log('called arcs')

    if (relationSettings && relationSettings.showAsArcs) {
        for (let i = 0; i < relationSettings.showAsArcs.length; i++) {
            if (relationSettings.showAsArcs[i].relation == rel) {
                return relationSettings.showAsArcs[i].showAsArcs
            }
        }
    } else {
        relationSettings.showAsArcs = []
    }
    relationSettings.showAsArcs.push({ type: rel, showAsArcs: true })
    return true
}

updateShowAsArcs = function (rel, newVal) {
    if (relationSettings.showAsArcs) {
        for (let i = 0; i < relationSettings.showAsArcs.length; i++) {
            if (relationSettings.showAsArcs[i].relation == rel) {
                relationSettings.showAsArcs[i].showAsArcs = newVal
                return
            }
        }
        relationSettings.showAsArcs.push({ relation: rel, showAsArcs: newVal })
        return
    }
    relationSettings.showAsArcs = []
    relationSettings.showAsArcs.push({ relation: rel, showAsArcs: newVal })
}

/**
 * Retrieves the show as attributes property of a relation, initializing to
 * false if undefined.
 *
 * @param {String} rel the relation for which to get the property
 * @returns {String} the value assigned to the property
 */
isShowAsAttributesOn = function (rel) {
    if (relationSettings.showAsAttributes) {
        for (let i = 0; i < relationSettings.showAsAttributes.length; i++) {
            if (relationSettings.showAsAttributes[i].relation == rel) {
                return relationSettings.showAsAttributes[i].showAsAttributes
            }
        }
    } else {
        relationSettings.showAsAttributes = []
    }
    relationSettings.showAsAttributes.push({ type: rel, showAsAttributes: false })
    return false
}

updateShowAsAttributes = function (rel, newVal) {
    if (relationSettings.showAsAttributes) {
        for (let i = 0; i < relationSettings.showAsAttributes.length; i++) {
            if (relationSettings.showAsAttributes[i].relation == rel) {
                relationSettings.showAsAttributes[i].showAsAttributes = newVal
                return
            }
        }
        relationSettings.showAsAttributes.push({ relation: rel, showAsAttributes: newVal })
        return
    }
    relationSettings.showAsAttributes = []
    relationSettings.showAsAttributes.push({ relation: rel, showAsAttributes: newVal })
}

propagateAttributes = function (rel, newVal) {
    if (cy) {
        const edges = cy.edges(`[relation='${rel}']`)
        if (newVal) {
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
        }
    }
}
