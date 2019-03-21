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
        $('#atomLabelSettings').val(atomSettings.getAtomLabel(selectedSig))

        if (!isSubset) {
            $('#projectOverSig').prop('checked', $.inArray(selectedSig, currentlyProjectedSigs) > -1)
            const unconnectedNodes = atomSettings.getHideUnconnectedNodes(selectedSig)

            if (unconnectedNodes == 'inherit') {
                $('#atomHideUnconnectedNodes').prop('disabled', true)
                $('#inheritHideUnconnectedNodes').prop('checked', true)
                $('#atomHideUnconnectedNodes').prop('checked', atomSettings.getInheritedHideUnconnectedNodes(selectedSig))
            } else {
                $('#atomHideUnconnectedNodes').prop('disabled', false)
                $('#inheritHideUnconnectedNodes').prop('checked', false)
                $('#atomHideUnconnectedNodes').prop('checked', unconnectedNodes)
            }

            const displayNodesNumber = atomSettings.getDisplayNodesNumber(selectedSig)

            if (displayNodesNumber == 'inherit') {
                $('#displayNodesNumber').prop('disabled', true)
                $('#inheritDisplayNodesNumber').prop('checked', true)
                $('#displayNodesNumber').prop('checked', atomSettings.getInheritedDisplayNodesNumber(selectedSig))
            } else {
                $('#displayNodesNumber').prop('disabled', false)
                $('#inheritDisplayNodesNumber').prop('checked', false)
                $('#displayNodesNumber').prop('checked', displayNodesNumber)
            }

            const visibility = atomSettings.getAtomVisibility(selectedSig)
            if (visibility == 'inherit') {
                const inheritedVisibility = atomSettings.getInheritedAtomVisibility(selectedSig)
                $('#inheritHideNodes').prop('checked', true)
                $('#hideNodes').prop('checked', inheritedVisibility)
                $('#hideNodes').prop('disabled', true)
            } else {
                $('#inheritHideNodes').prop('checked', false)
                $('#hideNodes').prop('checked', visibility)
                $('#hideNodes').prop('disabled', false)
            }
        }
        $('#atomColorSettings').val(atomSettings.getAtomColor(selectedSig))
        $('#atomShapeSettings').val(atomSettings.getAtomShape(selectedSig))
        $('#atomBorderSettings').val(atomSettings.getAtomBorder(selectedSig))
    }
}

Template.atomSettings.events({
    'change #atomLabelSettings'(event) {
        const selectedSig = Session.get('selectedSig')
        atomSettings.updateAtomLabel(selectedSig, event.target.value)
        refreshGraph()
    },

    'change #atomColorSettings'(event) {
        const selectedSig = Session.get('selectedSig')
        atomSettings.updateAtomColor(selectedSig, event.target.value)
        refreshGraph()
    },

    'change #atomShapeSettings'(event) {
        const selectedSig = Session.get('selectedSig')
        atomSettings.updateAtomShape(selectedSig, event.target.value)
        refreshGraph()
    },
    'change #atomBorderSettings'(event) {
        const selectedSig = Session.get('selectedSig')
        atomSettings.updateAtomBorder(selectedSig, event.target.value)
        refreshGraph()
    },
    'change #hideNodes'(event) {
        const selectedSig = Session.get('selectedSig')
        atomSettings.updateAtomVisibility(selectedSig, $(event.target).is(':checked'))
        refreshGraph()
        applyCurrentLayout()
    },
    'change #inheritHideNodes'(event) {
        const selectedSig = Session.get('selectedSig')
        if ($(event.target).is(':checked')) {
            $('#hideNodes').prop('disabled', true)
            atomSettings.updateAtomVisibility(selectedSig, 'inherit')
        } else {
            $('#hideNodes').prop('disabled', false)
        }
        const inheritedVisibility = atomSettings.getInheritedAtomVisibility(selectedSig)
        $('#hideNodes').prop('checked', inheritedVisibility)
        refreshGraph()
        applyCurrentLayout()
    },
    'change #atomHideUnconnectedNodes'(event) {
        const selectedSig = Session.get('selectedSig')
        atomSettings.updateHideUnconnectedNodes(selectedSig, $(event.target).is(':checked'))
        refreshGraph()
        applyCurrentLayout()
    },
    'change #inheritHideUnconnectedNodes'(event) {
        const selectedSig = Session.get('selectedSig')
        if ($(event.target).is(':checked')) {
            $('#atomHideUnconnectedNodes').prop('disabled', true)
            atomSettings.updateHideUnconnectedNodes(selectedSig, 'inherit')
        } else {
            $('#atomHideUnconnectedNodes').prop('disabled', false)
        }
        const inheritedVisibility = atomSettings.getInheritedHideUnconnectedNodes(selectedSig)
        $('#atomHideUnconnectedNodes').prop('checked', inheritedVisibility)
        refreshGraph()
        applyCurrentLayout()
    },
    'change #displayNodesNumber'(event) {
        const selectedSig = Session.get('selectedSig')
        atomSettings.updateDisplayNodesNumber(selectedSig, $(event.target).is(':checked'))
        refreshGraph()
    },
    'change #inheritDisplayNodesNumber'(event) {
        const selectedSig = Session.get('selectedSig')
        if ($(event.target).is(':checked')) {
            $('#displayNodesNumber').prop('disabled', true)
            atomSettings.updateDisplayNodesNumber(selectedSig, 'inherit')
        } else {
            $('#displayNodesNumber').prop('disabled', false)
        }
        const displayNodesNumber = atomSettings.getInheritedDisplayNodesNumber(selectedSig)
        $('#displayNodesNumber').prop('checked', displayNodesNumber)
        refreshGraph()
    },
    'change #projectOverSig'(event) {
        const selectedSig = Session.get('selectedSig')
        try {
            if (event.currentTarget.checked)addSigToProjection(selectedSig)
            else removeSigFromProjection(selectedSig)
        } catch (err) {
            console.error(err)
        }
    }
})

Template.atomSettings.onRendered(() => {
    $('.atom-settings').hide()
})
