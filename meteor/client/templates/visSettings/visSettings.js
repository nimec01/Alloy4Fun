Template.visSettings.helpers({

	/** 
     * Whether the instance is empty.
     */
    emptyUniverse() {
        return Session.get('empty-instance');
    },

});

Template.visSettings.events({
    'click #settings-button'() {
        $('.settings-panel').toggleClass('open');
    },
});

Template.visSettings.onRendered(() => {
    // Add styling to scroll bar on theme settings
    $('.scroll-settings').slimScroll({
        height: '380px',
    });
});
