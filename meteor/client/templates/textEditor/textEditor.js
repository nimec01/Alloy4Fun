import {initializeAlloyEditor} from '/imports/editor/EditorInitializer';

Template.textEditor.onRendered(function () {
    textEditor = initializeAlloyEditor(document.getElementById("editor"));
});

