Template.rightClickOptionsMenu.helpers({
    getRightClickTargetType() {
        target = Session.get('rightClickType');
        if (!target) target = Session.get('rightClickRelation');
        return target;
    }
});

// updates the content of the right-click menu, depending on whether edge or atom,
// with the current state of each property
updateRightClickContent = function() {
    selectedType = Session.get('rightClickType');
    if (selectedType) {
        $('.right-click-color-picker').val(getAtomColor(selectedType));
        $('.right-click-shape-picker').val(getAtomShape(selectedType));
    } else {
        selectedType = Session.get('rightClickRelation');
        if (selectedType) {
            $('.right-click-color-picker').val(getRelationColor(selectedType));
        }
    }

    // disable current model link since theme may change
    $('.permalink > button').prop('disabled', false);
    $('#url-permalink').empty()
}

Template.rightClickOptionsMenu.events({
    'change .right-click-color-picker'(event) {
        selectedType = Session.get('rightClickType');
        if (selectedType) {
            cy.nodes(`[type='${selectedType}']`).data({ color: event.target.value });
            updateAtomColor(selectedType, event.target.value);            
        } else {
            selectedType = Session.get('rightClickRelation');
            cy.edges(`[relation='${selectedType}']`).data({ color: event.target.value });
            updateRelationColor(selectedType, event.target.value);
        }
        refreshGraph();
    },
    'change .right-click-shape-picker'(event) {
        const selectedType = Session.get('rightClickType');
        cy.nodes(`[type='${selectedType}']`).data({ shape: event.target.value });
        updateAtomShape(selectedType, event.target.value);
        refreshGraph();
    },
    'click #rightClickProject'() {
        const selectedType = Session.get('rightClickType');
        try {
            if (currentlyProjectedTypes.indexOf(selectedType) == -1)addTypeToProjection(selectedType);
            else removeTypeFromProjection(selectedType);
            $('#optionsMenu').hide();
            // TODO simular click na parte branca para limpar o checkBox

            // eventFire(document.getElementById(''), 'click');
        } catch (err) {
            console.log(err);
        }
    },
});

Template.rightClickOptionsMenu.onRendered(() => {
    $('#optionsMenu').hide();
});
