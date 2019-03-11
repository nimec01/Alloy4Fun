import {
    removeSigFromProjection,
    addSigToProjection
} from "../../../lib/visualizer/projection"

Template.atomSettings.helpers({
    getSig() {
        const type = Session.get('selectedSig');
        return type || undefined;
    },
    notUniv() {
        const type = Session.get('selectedSig');
        return (type && type != 'univ');
    }
});

// updates the content of the signatures pane in the settings sidebar, including the
// current state of each property
updateOptionContentSigs = function() {
    const selectedSig = Session.get('selectedSig');
    if (selectedSig && selectedSig == 'univ')$('.not-for-univ').hide();
    else $('.not-for-univ').show();
    const isSubset = selectedSig ? selectedSig.indexOf(':') != -1 : false;
    if (selectedSig) {
        $('#atomLabelSettings').val(getAtomLabel(selectedSig));

        if (!isSubset) {
            $('#projectOverSig').prop('checked', $.inArray(selectedSig, currentlyProjectedSigs) > -1);
            const unconnectedNodes = getUnconnectedNodesValue(selectedSig);

            if (unconnectedNodes == 'inherit') {
                $('#atomHideUnconnectedNodes').prop('disabled', true);
                $('#inheritHideUnconnectedNodes').prop('checked', true);
                $('#atomHideUnconnectedNodes').prop('checked', getInheritedHideUnconnectedNodesValue(selectedSig) == 'true');
            } else {
                $('#atomHideUnconnectedNodes').prop('disabled', false);
                $('#inheritHideUnconnectedNodes').prop('checked', false);
                $('#atomHideUnconnectedNodes').prop('checked', unconnectedNodes == 'true');
            }

            const displayNodesNumber = getDisplayNodesNumberValue(selectedSig);

            if (displayNodesNumber == 'inherit') {
                $('#displayNodesNumber').prop('disabled', true);
                $('#inheritDisplayNodesNumber').prop('checked', true);
                $('#displayNodesNumber').prop('checked', getInheritedDisplayNodesNumberValue(selectedSig) == 'true');
            } else {
                $('#displayNodesNumber').prop('disabled', false);
                $('#inheritDisplayNodesNumber').prop('checked', false);
                $('#displayNodesNumber').prop('checked', displayNodesNumber == 'true');
            }

            const visibility = getAtomVisibility(selectedSig);

            if (visibility == 'inherit') {
                const inheritedVisibility = getInheritedAtomVisibility(selectedSig);
                $('#inheritHideNodes').prop('checked', true);
                $('#hideNodes').prop('checked', inheritedVisibility == 'visible');
                $('#hideNodes').prop('disabled', true);
            } else {
                $('#inheritHideNodes').prop('checked', false);
                $('#hideNodes').prop('checked', visibility == 'invisible');
                $('#hideNodes').prop('disabled', false);
            }
        }
        $('#atomColorSettings').val(getAtomColor(selectedSig));
        $('#atomShapeSettings').val(getAtomShape(selectedSig));
        $('#atomBorderSettings').val(getAtomBorder(selectedSig));
    }
}

Template.atomSettings.events({
    'change #atomLabelSettings'(event) {
        const selectedSig = Session.get('selectedSig');
        cy.nodes(`[type='${selectedSig}']`).data({ label: event.target.value });
        updateAtomLabel(selectedSig, event.target.value);
        refreshGraph();
        refreshAttributes();
    },

    'change #atomColorSettings'(event) {
        const selectedSig = Session.get('selectedSig');
        cy.nodes(`[type='${selectedSig}']`).data({ color: event.target.value });
        updateAtomColor(selectedSig, event.target.value);
        refreshGraph();
    },

    'change #atomShapeSettings'(event) {
        const selectedSig = Session.get('selectedSig');
        cy.nodes(`[type='${selectedSig}']`).data({ shape: event.target.value });
        updateAtomShape(selectedSig, event.target.value);
        refreshGraph();
    },
    'change #atomBorderSettings'(event) {
        const selectedSig = Session.get('selectedSig');
        cy.nodes(`[type='${selectedSig}']`).data({ border: event.target.value });
        updateAtomBorder(selectedSig, event.target.value);
        refreshGraph();
    },
    'change #atomHideUnconnectedNodes'(event) {
        const selectedSig = Session.get('selectedSig');
        setUnconnectedNodesValue(selectedSig, $(event.target).is(':checked').toString());
        updateUnconnectedNodes(selectedSig, $(event.target).is(':checked').toString());
        refreshGraph();
    },
    'change #inheritHideUnconnectedNodes'(event) {
        const selectedSig = Session.get('selectedSig');
        if ($(event.target).is(':checked')) {
            $('#atomHideUnconnectedNodes').prop('disabled', true);
            updateUnconnectedNodes(selectedSig, 'inherit');
        } else {
            $('#atomHideUnconnectedNodes').prop('disabled', false);
            const hideUnconnectedNodes = getInheritedHideUnconnectedNodesValue(selectedSig);
            updateUnconnectedNodes(selectedSig, hideUnconnectedNodes);
            $('#atomHideUnconnectedNodes').prop('checked', hideUnconnectedNodes == 'true');
        }
    },
    'change #inheritDisplayNodesNumber'(event) {
        const selectedSig = Session.get('selectedSig');
        const displayNodesNumber = getInheritedDisplayNodesNumberValue(selectedSig);
        if ($(event.target).is(':checked')) {
            $('#displayNodesNumber').prop('disabled', true);
            updateDisplayNodesNumber(selectedSig, 'inherit');
            setDisplayNodesNumberValue(selectedSig, displayNodesNumber);
        } else {
            $('#displayNodesNumber').prop('disabled', false);
            updateDisplayNodesNumber(selectedSig, displayNodesNumber);
            $('#displayNodesNumber').prop('checked', displayNodesNumber == 'true');
        }
        refreshGraph();
    },
    'change #displayNodesNumber'(event) {
        const selectedSig = Session.get('selectedSig');
        setDisplayNodesNumberValue(selectedSig, $(event.target).is(':checked').toString());
        updateDisplayNodesNumber(selectedSig, $(event.target).is(':checked').toString());
        refreshGraph();
    },
    'change #hideNodes'(event) {
        const selectedSig = Session.get('selectedSig');
        setAtomVisibility(selectedSig, $(event.target).is(':checked') ? 'invisible' : 'visible');
        updateUnconnectedNodes(selectedSig, $(event.target).is(':checked') ? 'invisible' : 'visible');
        refreshGraph();
    },
    'change #inheritHideNodes'(event) {
        const selectedSig = Session.get('selectedSig');
        const inheritedVisibility = selectedSig == 'univ' ? getAtomVisibility('univ') : getInheritedAtomVisibility(selectedSig);
        if ($(event.target).is(':checked')) {
            $('#hideNodes').prop('disabled', true);
            updateAtomVisibility(selectedSig, 'inherit');
            setAtomVisibility(selectedSig, inheritedVisibility);
        } else {
            $('#hideNodes').prop('disabled', false);
            updateAtomVisibility(selectedSig, inheritedVisibility);
            $('#hideNodes').prop('checked', inheritedVisibility == 'invisible');
        }
        refreshGraph();
    },
    'change #projectOverSig'(event) {
        const selectedSig = Session.get('selectedSig');
        try {
            if (event.currentTarget.checked)addSigToProjection(selectedSig);
            else removeSigFromProjection(selectedSig);
        } catch (err) {
            console.log(err);
        }
    },
});

Template.atomSettings.onRendered(() => {
    $('.atom-settings').hide();
});
