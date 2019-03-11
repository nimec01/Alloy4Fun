import {
    currentFramePositionToString,
    savePositions,
    project
} from "../../../lib/visualizer/projection"

Template.frameNavigation.helpers({

});

Template.frameNavigation.events({
    'click #nextFrame'() {
        if ($("#nextFrame > button").is(":disabled")) return

        const type = $('.framePickerTarget')[0].value;
        currentFramePosition[type]++;
        if (currentFramePosition[type] == lastFrame(type)) {
            $('#nextFrame').prop('disabled',true);
        }
        $('#previousFrame').prop('disabled',false);
        $('.current-frame').html(currentFramePositionToString());
        savePositions();
        project();
    },
    'click #previousFrame'() {
        if ($("#previousFrame > button").is(":disabled")) return

        const type = $('.framePickerTarget')[0].value;
        currentFramePosition[type]--;
        if (currentFramePosition[type] == 0) {
            $('#previousFrame').prop('disabled',true);
        }
        $('#nextFrame').prop('disabled',false);
        $('.current-frame').html(currentFramePositionToString());
        savePositions();
        project();
    },
    'change .framePickerTarget'(event) {
        const selectedType = event.target.value;
        const currentAtom = currentFramePosition[selectedType];
        $('#nextFrame').prop('disabled',false);
        $('#previousFrame').prop('disabled',false);
        if (currentAtom == lastFrame(selectedType))$('#nextFrame').prop('disabled',true);
        if (currentAtom == 0)$('#previousFrame').prop('disabled',true);
    },
});

Template.frameNavigation.onRendered(() => {
    $('.frame-navigation').hide();
});

// retrieves the last index of atoms of a given type, used for frame navigation
lastFrame = function (type) {
    return allNodes.nodes(`[type='${type}']`).length - 1;
};
