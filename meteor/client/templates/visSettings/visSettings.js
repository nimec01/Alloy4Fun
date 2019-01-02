Template.visSettings.helpers({
});

Template.visSettings.events({
    'click #settings-button': function () {
        $('.settings-panel').toggleClass('open');
    }
});

Template.visSettings.onRendered(function () {
    //Add styling to scroll bar on theme settings
    $('.scroll-settings').slimScroll({
        height: '380px'
    });

});

