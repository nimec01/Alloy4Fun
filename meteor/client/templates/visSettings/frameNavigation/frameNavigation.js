Template.frameNavigation.helpers({

});

Template.frameNavigation.events({
    'click #nextFrame.enabled'() {
        const type = $('.framePickerTarget')[0].value;
        currentFramePosition[type]++;
        if (currentFramePosition[type] == lastFrame(type)) {
            $('#nextFrame.enabled').removeClass('enabled');
        }
        $('#previousFrame').addClass('enabled');
        $('.current-frame').html(currentFramePositionToString());
        savePositions();
        project();
    },
    'click #previousFrame.enabled'() {
        const type = $('.framePickerTarget')[0].value;
        currentFramePosition[type]--;
        if (currentFramePosition[type] == 0) {
            $('#previousFrame.enabled').removeClass('enabled');
        }
        $('#nextFrame').addClass('enabled');
        $('.current-frame').html(currentFramePositionToString());
        savePositions();
        project();
    },
    'change .framePickerTarget'(event) {
        const selectedType = event.target.value;
        console.log(`currentFramePosition: ${currentFramePosition}`);
        const currentAtom = currentFramePosition[selectedType];
        console.log(`currentAtom: ${currentAtom}`);
        $('#nextFrame').addClass('enabled');
        $('#previousFrame').addClass('enabled');
        if (currentAtom == lastFrame(selectedType))$('#nextFrame').removeClass('enabled');
        if (currentAtom == 0)$('#previousFrame').removeClass('enabled');
    },
});

Template.frameNavigation.onRendered(() => {
    $('.frame-navigation').hide();
});

// retrieves the last index of atoms of a given type, used for frame navigation
lastFrame = function (type) {
    return allNodes.nodes(`[type='${type}']`).length - 1;
};
