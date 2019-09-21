import { removeSigFromProjection,
    addSigToProjection } from '../../../lib/visualizer/projection'

Template.sigSettings.helpers({
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
        if (!isSubset) {
            $('#projectOverSig').prop('checked', $.inArray(selectedSig, currentlyProjectedSigs) > -1)

            const visibility = sigSettings.getAtomVisibility(selectedSig)
            if (visibility == 'inherit') {
                const inheritedVisibility = sigSettings.getInheritedAtomVisibility(selectedSig)
                $('#inheritHideNodes').prop('checked', true)
                $('#hideNodes').prop('checked', inheritedVisibility)
                $('#hideNodes').prop('disabled', true)
            } else {
                $('#inheritHideNodes').prop('checked', false)
                $('#hideNodes').prop('checked', visibility)
                $('#hideNodes').prop('disabled', false)
            }
        }
        $('#atomColorSettings').val(sigSettings.getAtomColor(selectedSig))
        $('#atomShapeSettings').val(sigSettings.getAtomShape(selectedSig))
        $('#atomBorderSettings').val(sigSettings.getAtomBorder(selectedSig))
    }
}

Template.sigSettings.events({

    'change #atomColorSettings'(event) {
        const selectedSig = Session.get('selectedSig')
        sigSettings.updateAtomColor(selectedSig, event.target.value)
        refreshGraph()
    },

    'change #atomShapeSettings'(event) {
        const selectedSig = Session.get('selectedSig')
        sigSettings.updateAtomShape(selectedSig, event.target.value)
        refreshGraph()
    },
    'change #atomBorderSettings'(event) {
        const selectedSig = Session.get('selectedSig')
        sigSettings.updateAtomBorder(selectedSig, event.target.value)
        refreshGraph()
    },
    'change #hideNodes'(event) {
        const selectedSig = Session.get('selectedSig')
        sigSettings.updateAtomVisibility(selectedSig, $(event.target).is(':checked'))
        refreshGraph()
        applyCurrentLayout()
    },
    'change #inheritHideNodes'(event) {
        const selectedSig = Session.get('selectedSig')
        if ($(event.target).is(':checked')) {
            $('#hideNodes').prop('disabled', true)
            sigSettings.updateAtomVisibility(selectedSig, 'inherit')
        } else {
            $('#hideNodes').prop('disabled', false)
        }
        const inheritedVisibility = sigSettings.getInheritedAtomVisibility(selectedSig)
        $('#hideNodes').prop('checked', inheritedVisibility)
        refreshGraph()
        applyCurrentLayout()
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

Template.sigSettings.onRendered(() => {
    $('.atom-settings').hide()
})
