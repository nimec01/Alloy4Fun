Template.elementSelection.helpers({

});

Template.elementSelection.events({
    'click'(event) {
        updateOptionContentTypes();
        updateOptionContentRelations();

        // disable current model link since theme may change
        $('.permalink > button').prop('disabled', false);
        $('#url-permalink').empty()
    }
});

Template.elementSelection.onRendered(() => {
    selectAtomElement = $('#selectAtom').selectize({
        delimiter: ',',
        hideSelected: true,
        create: false,
    })[0];

    $('.wrapper-select-atom > div > div.selectize-input').append("<p class='select-label'>Signatures</p>");

    selectAtomElement.selectize.on('item_add', (value, item) => {
        item.on('click', () => {
            Session.set('selectedType', value);
            $('.general-settings').slideUp();
            $('.relation-settings').slideUp();
            $('.atom-settings').slideDown();

            $('.projection-settings').show();
            $('.hide-unconnected-settings').show();
            $('.hide-nodes-settings').show();
            $('.number-nodes-settings').show();
        });
    });

    selectRelationElement = $('#selectRelation').selectize({
        delimiter: ',',
        hideSelected: true,
        create: false,
    })[0];

    selectRelationElement.selectize.on('item_add', (value, item) => {
        item.on('click', () => {
            Session.set('selectedRelation', value);
            $('.general-settings').slideUp();
            $('.atom-settings').slideUp();
            $('.relation-settings').slideDown();
        });
    });

    $('.wrapper-select-relation > div > div.selectize-input').append("<p class='select-label'>Relations</p>");


    selectSubset = $('#selectSubset').selectize({
        delimiter: ',',
        hideSelected: true,
        create: false,
    })[0];

    $('.wrapper-select-subset > div > div.selectize-input').append("<p class='select-label'>Subsets</p>");

    selectSubset.selectize.on('item_add', (value, item) => {
        item.on('click', () => {
            Session.set('selectedType', value);
            $('.general-settings').slideUp();
            $('.relation-settings').slideUp();
            $('.atom-settings').slideDown();

            $('.projection-settings').hide();
            $('.hide-unconnected-settings').hide();
            $('.hide-nodes-settings').hide();
            $('.number-nodes-settings').hide();
        });
    });
});
