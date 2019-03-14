Template.relationSettings.helpers({
    getRelation() {
        return Session.get('selectedRelation')
    }
})

// updates the content of the relations pane in the settings sidebar, including the
// current state of each property
updateOptionContentRelations = function () {
    const selectedRelation = Session.get('selectedRelation')
    if (selectedRelation) {
        $('#relationColorSettings').val(getEdgeColor(selectedRelation))
        $('#relationLabelSettings').val(getEdgeLabel(selectedRelation))
        $('#showAsArcs').prop('checked', isShowAsArcsOn(selectedRelation))
        $('#showAsAttributes').prop('checked', isShowAsAttributesOn(selectedRelation))
        $('#relationEdgeStyleSettings').val(getEdgeStyle(selectedRelation))
    }
}

Template.relationSettings.events({
    'change #relationLabelSettings'(event) {
        const selectedRelation = Session.get('selectedRelation')
        cy.edges(`[relation='${selectedRelation}']`).data({ label: event.target.value })
        updateEdgeLabel(selectedRelation, event.target.value)
        refreshGraph()
    },

    'change #relationColorSettings'(event) {
        const selectedRelation = Session.get('selectedRelation')
        cy.edges(`[relation='${selectedRelation}']`).data({ color: event.target.value })
        updateEdgeColor(selectedRelation, event.target.value)
    },
    'change #showAsArcs'(event) {
        const selectedRelation = Session.get('selectedRelation')
        updateShowAsArcs(selectedRelation, $(event.target).is(':checked'))
        refreshGraph()
    },
    'change #showAsAttributes'(event) {
        const selectedRelation = Session.get('selectedRelation')
        updateShowAsAttributes(selectedRelation, $(event.target).is(':checked'))
        refreshGraph()
    },
    'change #relationEdgeStyleSettings'(event) {
        const selectedRelation = Session.get('selectedRelation')
        cy.edges(`[relation='${selectedRelation}']`).data({ edgeStyle: event.target.value })
        updateEdgeStyle(selectedRelation, event.target.value)
        refreshGraph()
    }
})

Template.relationSettings.onRendered(() => {
    $('.relation-settings').hide()
})
