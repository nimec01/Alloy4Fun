Template.elementSelection.onRendered(() => {
    $('#layoutPicker').val(generalSettings.getLayout())
})

Template.generalSettings.events({
    'change #layoutPicker'(event) {
        const currentLayout = event.target.value
        generalSettings.updateLayout(currentLayout)
        applyCurrentLayout()
    }
})
