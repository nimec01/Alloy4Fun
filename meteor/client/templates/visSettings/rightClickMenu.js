import { themeChanged } from '../../lib/editor/state'
import { removeSigFromProjection,
    addSigToProjection } from '../../lib/visualizer/projection'


Template.rightClickMenu.helpers({
    /**
     * The target of the right click menu, may be a sig or a relation.
     */
    getRightClickLabel() {
        let target = Session.get('rightClickSig')
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
    let selected = Session.get('rightClickSig')
    if (selected) {
        $('.changeAtomColorPicker').val(sigSettings.getAtomColor(selected))
        $('.changeAtomShapePicker').val(sigSettings.getAtomShape(selected))
        $('.changeAtomBorderPicker').val(sigSettings.getAtomBorder(selected))
    } else {
        selected = Session.get('rightClickRel')
        if (selected) {
            $('.changeAtomColorPicker').val(relationSettings.getEdgeColor(selected))
        }
    }
    themeChanged()
}

Template.rightClickMenu.events({
    'change .changeAtomColorPicker'(event) {
        let selected = Session.get('rightClickSig')
        if (selected) {
            sigSettings.updateAtomColor(selected, event.target.value)
        } else {
            selected = Session.get('rightClickRel')
            relationSettings.updateEdgeColor(selected, event.target.value)
        }
        refreshGraph()
    },
    'change .changeAtomShapePicker'(event) {
        const selected = Session.get('rightClickSig')
        sigSettings.updateAtomShape(selected, event.target.value)
        refreshGraph()
    },
    'change .changeAtomBorderPicker'(event) {
        let selected = Session.get('rightClickSig')
        if (selected) {
            sigSettings.updateAtomBorder(selected, event.target.value)
        } else {
            selected = Session.get('rightClickRel')
            relationSettings.updateEdgeStyle(selected, event.target.value)
        }
        refreshGraph()
    },
    'click #hideAtom'() {
        let selected = Session.get('rightClickSig')
        if (selected) {
            const val = sigSettings.getInheritedAtomVisibility(selected)
            sigSettings.updateAtomVisibility(selected, !val)
        } else {
            selected = Session.get('rightClickRel')
            const val = relationSettings.isShowAsArcsOn(selected)
            relationSettings.updateShowAsArcs(selected, !val)            
        }
        refreshGraph()
        applyCurrentLayout()
    },
    'click #rightClickProject'() {
        const selected = Session.get('rightClickSig')
        try {
            if (currentlyProjectedSigs.indexOf(selected) == -1) addSigToProjection(selected)
            else removeSigFromProjection(selected)
        } catch (err) {
            console.error(err)
        }
    }
})

Template.rightClickMenu.onRendered(() => {
    $('#optionsMenu').hide()
})
