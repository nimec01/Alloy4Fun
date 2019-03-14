import { removeSigFromProjection,
    addSigToProjection } from '../../../lib/visualizer/projection'

Template.atomSettings.helpers({
    getSig() {
        const type = Session.get('selectedSig')
        return type || undefined
    },
    notUniv() {
        const type = Session.get('selectedSig')
        return (type && type !== 'univ')
    }
})

// updates the content of the signatures pane in the settings sidebar, including the
// current state of each property
updateOptionContentSigs = function () {
    const selectedSig = Session.get('selectedSig')
    if (selectedSig && selectedSig == 'univ')$('.not-for-univ').hide()
    else $('.not-for-univ').show()
    const isSubset = selectedSig ? selectedSig.indexOf(':') != -1 : false
    if (selectedSig) {
        $('#atomLabelSettings').val(getAtomLabel(selectedSig))

        if (!isSubset) {
            $('#projectOverSig').prop('checked', $.inArray(selectedSig, currentlyProjectedSigs) > -1)
            const unconnectedNodes = getHideUnconnectedNodes(selectedSig)

            if (unconnectedNodes == 'inherit') {
                $('#atomHideUnconnectedNodes').prop('disabled', true)
                $('#inheritHideUnconnectedNodes').prop('checked', true)
                $('#atomHideUnconnectedNodes').prop('checked', getInheritedHideUnconnectedNodes(selectedSig))
            } else {
                $('#atomHideUnconnectedNodes').prop('disabled', false)
                $('#inheritHideUnconnectedNodes').prop('checked', false)
                $('#atomHideUnconnectedNodes').prop('checked', unconnectedNodes)
            }

            const displayNodesNumber = getDisplayNodesNumber(selectedSig)

            if (displayNodesNumber == 'inherit') {
                $('#displayNodesNumber').prop('disabled', true)
                $('#inheritDisplayNodesNumber').prop('checked', true)
                $('#displayNodesNumber').prop('checked', getInheritedDisplayNodesNumber(selectedSig))
            } else {
                $('#displayNodesNumber').prop('disabled', false)
                $('#inheritDisplayNodesNumber').prop('checked', false)
                $('#displayNodesNumber').prop('checked', displayNodesNumber)
            }

            const visibility = getAtomVisibility(selectedSig)
            if (visibility == 'inherit') {
                const inheritedVisibility = getInheritedAtomVisibility(selectedSig)
                $('#inheritHideNodes').prop('checked', true)
                $('#hideNodes').prop('checked', inheritedVisibility)
                $('#hideNodes').prop('disabled', true)
            } else {
                $('#inheritHideNodes').prop('checked', false)
                $('#hideNodes').prop('checked', visibility)
                $('#hideNodes').prop('disabled', false)
            }
        }
        $('#atomColorSettings').val(getAtomColor(selectedSig))
        $('#atomShapeSettings').val(getAtomShape(selectedSig))
        $('#atomBorderSettings').val(getAtomBorder(selectedSig))
    }
}

Template.atomSettings.events({
    'change #atomLabelSettings'(event) {
        const selectedSig = Session.get('selectedSig')
        cy.nodes(`[type='${selectedSig}']`).data({ label: event.target.value })
        updateAtomLabel(selectedSig, event.target.value)
        refreshGraph()
    },

    'change #atomColorSettings'(event) {
        const selectedSig = Session.get('selectedSig')
        cy.nodes(`[type='${selectedSig}']`).data({ color: event.target.value })
        updateAtomColor(selectedSig, event.target.value)
        refreshGraph()
    },

    'change #atomShapeSettings'(event) {
        const selectedSig = Session.get('selectedSig')
        cy.nodes(`[type='${selectedSig}']`).data({ shape: event.target.value })
        updateAtomShape(selectedSig, event.target.value)
        refreshGraph()
    },
    'change #atomBorderSettings'(event) {
        const selectedSig = Session.get('selectedSig')
        cy.nodes(`[type='${selectedSig}']`).data({ border: event.target.value })
        updateAtomBorder(selectedSig, event.target.value)
        refreshGraph()
    },
    'change #atomHideUnconnectedNodes'(event) {
        const selectedSig = Session.get('selectedSig')
        updateHideUnconnectedNodes(selectedSig, $(event.target).is(':checked'))
        refreshGraph()
    },
    'change #inheritHideUnconnectedNodes'(event) {
        const selectedSig = Session.get('selectedSig')
        const inheritedVisibility = getInheritedHideUnconnectedNodes(selectedSig)
        if ($(event.target).is(':checked')) {
            $('#atomHideUnconnectedNodes').prop('disabled', true)
            updateHideUnconnectedNodes(selectedSig, 'inherit')
        } else {
            $('#atomHideUnconnectedNodes').prop('disabled', false)
            updateHideUnconnectedNodes(selectedSig, inheritedVisibility)
        }
        $('#atomHideUnconnectedNodes').prop('checked', inheritedVisibility)
        refreshGraph()
    },
    'change #displayNodesNumber'(event) {
        const selectedSig = Session.get('selectedSig')
        updateDisplayNodesNumber(selectedSig, $(event.target).is(':checked'))
        refreshGraph()
    },
    'change #inheritDisplayNodesNumber'(event) {
        const selectedSig = Session.get('selectedSig')
        const displayNodesNumber = getInheritedDisplayNodesNumber(selectedSig)
        if ($(event.target).is(':checked')) {
            $('#displayNodesNumber').prop('disabled', true)
            updateDisplayNodesNumber(selectedSig, 'inherit')
        } else {
            $('#displayNodesNumber').prop('disabled', false)
            updateDisplayNodesNumber(selectedSig, displayNodesNumber)
        }
        $('#displayNodesNumber').prop('checked', displayNodesNumber)
        refreshGraph()
    },
    'change #hideNodes'(event) {
        const selectedSig = Session.get('selectedSig')
        updateAtomVisibility(selectedSig, $(event.target).is(':checked'))
        refreshGraph()
    },
    'change #inheritHideNodes'(event) {
        const selectedSig = Session.get('selectedSig')
        const inheritedVisibility = getInheritedAtomVisibility(selectedSig)
        if ($(event.target).is(':checked')) {
            $('#hideNodes').prop('disabled', true)
            updateAtomVisibility(selectedSig, 'inherit')
        } else {
            $('#hideNodes').prop('disabled', false)
            updateAtomVisibility(selectedSig, inheritedVisibility)
        }
        $('#hideNodes').prop('checked', inheritedVisibility)
        refreshGraph()
    },
    'change #projectOverSig'(event) {
        const selectedSig = Session.get('selectedSig')
        try {
            if (event.currentTarget.checked)addSigToProjection(selectedSig)
            else removeSigFromProjection(selectedSig)
        } catch (err) {
            console.log(err)
        }
    }
})

Template.atomSettings.onRendered(() => {
    $('.atom-settings').hide()
})
