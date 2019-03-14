import { themeChanged } from '../../lib/editor/state'
import { removeSigFromProjection,
    addSigToProjection } from '../../lib/visualizer/projection'


Template.rightClickMenu.helpers({
    /**
     * The target of the right click menu, may be a sig or a relation.
     */
    getRightClickLabel() {
        target = Session.get('rightClickSig')
        if (!target) target = Session.get('rightClickRel')
        return target
    },

    /**
     * Whether to show the sig theme options.
     */
    showSigProps() {
        return Session.get('rightClickSig') ? '' : 'hidden'
    },

    /**
     * Whether to show projection theme options.
     */
    showProjectionProp() {
        return Session.get('from-instance') ? 'hidden' : ''
    }
})

/**
 * Updates the content of the right-click menu, depending on whether edge or
 * atom, with the current state of each property.
 */
export function updateRightClickContent() {
    selected = Session.get('rightClickSig')
    if (selected) {
        $('.changeAtomColorPicker').val(getAtomColor(selected))
        $('.changeAtomShapePicker').val(getAtomShape(selected))
    } else {
        selected = Session.get('rightClickRel')
        if (selected) {
            $('.changeAtomColorPicker').val(getRelationColor(selected))
        }
    }
    themeChanged()
}

Template.rightClickMenu.events({
    'change .changeAtomColorPicker'(event) {
        selected = Session.get('rightClickSig')
        if (selected) {
            cy.nodes(`[type='${selected}']`).data({ color: event.target.value })
            updateAtomColor(selected, event.target.value)
        } else {
            selected = Session.get('rightClickRel')
            cy.edges(`[relation='${selected}']`).data({ color: event.target.value })
            updateRelationColor(selected, event.target.value)
        }
        refreshGraph()
    },
    'change .changeAtomShapePicker'(event) {
        const selected = Session.get('rightClickSig')
        cy.nodes(`[type='${selected}']`).data({ shape: event.target.value })
        updateAtomShape(selected, event.target.value)
        refreshGraph()
    },
    'click #rightClickProject'() {
        const selected = Session.get('rightClickSig')
        try {
            if (currentlyProjectedSigs.indexOf(selected) == -1) addSigToProjection(selected)
            else removeSigFromProjection(selected)
        } catch (err) {
            console.log(err)
        }
    }
})

Template.rightClickMenu.onRendered(() => {
    $('#optionsMenu').hide()
})
