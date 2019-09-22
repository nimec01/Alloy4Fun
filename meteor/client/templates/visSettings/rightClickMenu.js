import { themeChanged } from '../../lib/editor/state'
import { removeSigFromProjection,
    addSigToProjection } from '../../lib/visualizer/projection'


Template.rightClickMenu.helpers({
    /**
     * The target of the right click menu, may be a sig or a relation.
     */
    getRightClickLabels() {
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

    showRelProps() {
        return Session.get('rightClickRel') ? '' : 'hidden'
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
        $('.changeAtomColorPicker').each(function() {
            console.log($(this).val())
            $(this).val(sigSettings.getAtomColor($(this).attr("elm")))
        })
        $('.changeAtomShapePicker').each(function() {
            console.log($(this).val())
            $(this).val(sigSettings.getAtomShape($(this).attr("elm")))
        })
        $('.changeAtomBorderPicker').each(function() {
            console.log($(this).val())
            $(this).val(sigSettings.getAtomBorder($(this).attr("elm")))
        })
    } else {
        selected = Session.get('rightClickRel')
        if (selected) {
            $('.changeAtomColorPicker').each(function() {
                console.log($(this).val())
                $(this).val(relationSettings.getEdgeColor($(this).attr("elm")))
            })
        }
    }
    themeChanged()
}

Template.rightClickMenu.events({
    'change .changeAtomColorPicker'(event) {
        const elem = event.target.getAttribute("elm")
        if (Session.get('rightClickSig')) {
            sigSettings.updateAtomColor(elem, event.target.value)
        } else if (Session.get('rightClickrel')) {
            relationSettings.updateEdgeColor(elem, event.target.value)
        }
        refreshGraph()
    },
    'change .changeAtomShapePicker'(event) {
        const elem = event.target.getAttribute("elm")
        sigSettings.updateAtomShape(elem, event.target.value)
        refreshGraph()
    },
    'change .changeAtomBorderPicker'(event) {
        const elem = event.target.getAttribute("elm")
        if (Session.get('rightClickSig')) {
            sigSettings.updateAtomBorder(elem, event.target.value)
        } else if (Session.get('rightClickRel')) {
            relationSettings.updateEdgeStyle(elem, event.target.value)
        }
        refreshGraph()
    },
    'click #hideAtom'() {
        const elem = event.target.getAttribute("elm")
        if (Session.get('rightClickSig')) {
            const val = sigSettings.getInheritedAtomVisibility(elem)
            sigSettings.updateAtomVisibility(elem, !val)
        } else if (Session.get('rightClickRel')) {
            const val = relationSettings.isShowAsArcsOn(elem)
            relationSettings.updateShowAsArcs(elem, !val)            
        }
        refreshGraph()
        applyCurrentLayout()
    },
    'click #showAsAttribute'() {
        const elem = event.target.getAttribute("elm")
        const val = relationSettings.isShowAsAttributesOn(elem)
        relationSettings.updateShowAsAttributes(elem, !val)
        refreshGraph()
    },
    'click #rightClickProject'() {
        const elem = event.target.getAttribute("elm")
        try {
            if (currentlyProjectedSigs.indexOf(elem) == -1) addSigToProjection(elem)
            else removeSigFromProjection(elem)
        } catch (err) {
            console.error(err)
        }
    },
    'click #cssmenu li.has-sub>a'(event) {
        $(event.target).removeAttr('href')
        const element = $(event.target).parent('li')
        if (element.hasClass('open')) {
            element.removeClass('open')
            element.find('li').removeClass('open')
            element.find('ul').slideUp(200)
        } else {
            element.addClass('open')
            element.children('ul').slideDown(200)
            element.siblings('li').children('ul').slideUp(200)
            element.siblings('li').removeClass('open')
            element.siblings('li').find('li').removeClass('open')
            element.siblings('li').find('ul').slideUp(200)
        }
    }
})

Template.rightClickMenu.onRendered(() => {
    $('#optionsMenu').hide()
})
