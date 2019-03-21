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
        $('#relationColorSettings').val(relationSettings.getEdgeColor(selectedRelation))
        $('#relationLabelSettings').val(relationSettings.getEdgeLabel(selectedRelation))
        $('#showAsArcs').prop('checked', relationSettings.isShowAsArcsOn(selectedRelation))
        $('#showAsAttributes').prop('checked', relationSettings.isShowAsAttributesOn(selectedRelation))
        $('#relationEdgeStyleSettings').val(relationSettings.getEdgeStyle(selectedRelation))
    }
}

Template.relationSettings.events({
    'change #relationLabelSettings'(event) {
        const selectedRelation = Session.get('selectedRelation')
        relationSettings.updateEdgeLabel(selectedRelation, event.target.value)
        refreshGraph()
    },

    'change #relationColorSettings'(event) {
        const selectedRelation = Session.get('selectedRelation')
        relationSettings.updateEdgeColor(selectedRelation, event.target.value)
        refreshGraph()
    },
    'change #showAsArcs'(event) {
        const selectedRelation = Session.get('selectedRelation')
        relationSettings.updateShowAsArcs(selectedRelation, $(event.target).is(':checked'))
        refreshGraph()
        applyCurrentLayout()
    },
    'change #showAsAttributes'(event) {
        const selectedRelation = Session.get('selectedRelation')
        relationSettings.updateShowAsAttributes(selectedRelation, $(event.target).is(':checked'))
        refreshGraph()
    },
    'change #relationEdgeStyleSettings'(event) {
        const selectedRelation = Session.get('selectedRelation')
        relationSettings.updateEdgeStyle(selectedRelation, event.target.value)
        refreshGraph()
    }
})

Template.relationSettings.onRendered(() => {
    $('.relation-settings').hide()
})
