/**
 * Created by José Pereira on 11/1/2016.
 */
import CodeMirror from 'codemirror';
import {defineAlloyMode} from '/imports/editor/AlloyEditorMode';
import {defineAlloyChallengeMode} from '/imports/editor/AlloyChallengeMode';
import 'codemirror/theme/twilight.css';
import 'codemirror/lib/codemirror.css';

export {initializeAlloyCreateChallengeEditor, initializeAlloySolveChallengeEditor, initializeAlloyEditor};

//Editor initialization options.
var options = {
    //Display line numbers.
    lineNumbers: true,
    //Whether CodeMirror should scroll or wrap for long lines. Defaults to false (scroll).
    lineWrapping: true,
    styleActiveLine: true,
    //Highlight matching brackets when editor's standing next to them
    matchBrackets: true,
    //TODO: Allow choosing between multiple themes.
    theme: "twilight",
    //TODO: This is broken. Must be fixed to permit block folding.
    foldGutter: true,
    //Adds gutters to the editor. In this case a single one is added for the error icon placement
    gutters: ["error-gutter", "breakpoints"]
};

function initializeAlloyEditor(htmlElement){
    defineAlloyMode();

    var editor = initializeEditor(htmlElement, "alloy");
    //Text change event for the editor on alloy4fun/editor page
    editor.on('change', function (editor) {
        $(".qtip").remove();
        editor.clearGutter("error-gutter");
        //Delete previous existing permalink elements if existent.
        var permalink= document.getElementById("permalink");
        if(permalink)permalink.remove();
        $("#genInstanceUrl").hide();
        if ($.trim(editor.getValue()) == '') {
            //When editor is empty
            Session.set("commands",[]);
            $('#exec > button').prop('disabled', true);
            $('#next > button').prop('disabled', true);
            $('#prev > button').prop('disabled', true);
            $('.permalink > button').prop('disabled', true);
        } else {
            //Populate commands combo box
            editor.getCommands();
            if(Session.get("commands") && Session.get("commands").length>=0) {
                $('#instanceViewer').hide();
                $('#exec > button').prop('disabled', false);
                $('.permalink').prop('disabled', false);
                $('#next > button').prop('disabled', true);
                $('#prev > button').prop('disabled', true);
                $('.empty-univ').hide();
                $('.permalink > button').prop('disabled', false);
            }
        }
        Session.set("currentInstance",undefined);
        Session.set("instances",undefined);
        Session.set("projectableTypes",undefined);
    });
    return editor;
}

function initializeAlloyCreateChallengeEditor(htmlElement){
    defineAlloyChallengeMode();
    var editor = initializeEditor(htmlElement, "alloyChallenge");

    editor.on("gutterClick", function(cm, n) {
        var info = cm.lineInfo(n);
        cm.setGutterMarker(n, "breakpoints", info.gutterMarkers ? null : makeMarker());
    });
    //Text change event for the editor on alloy4fun/challenge pages
    editor.on('change', function () {
        editor.clearGutter("error-gutter");
        var permalink= document.getElementById("permalink");
        if(permalink)permalink.remove();
        if ($.trim(editor.getValue()) == '') {
            $('#saveAndShare').prop('disabled', true);
        } else {
            $('#saveAndShare').prop('disabled', false);
            editor.clearGutter("error-gutter");
            highlightLocksAndSecrets();
        }
    });
    editor.setSize("100%","100%");
    return editor;
}

function initializeAlloySolveChallengeEditor(htmlElement){
    defineAlloyMode();
    var editor = initializeEditor(htmlElement, "alloyChallenge");

    editor.on('change', function () {
        editor.clearGutter("error-gutter");
        var permalink= document.getElementById("permalink");
        if(permalink)permalink.remove();
        $("#shareSolution").prop('disabled', false);
        var history = Session.get("modelHistory");
        if(history){
            history.changed = true;
            Session.set("modelHistory", history);
        }else{
            Session.set("modelHistory", {id: Router.current().data()._id, changed: true});
        }
        $('#instanceViewer').hide();
        $('#instancenav').hide();
        $("#log").empty();

        if ($.trim(challengeEditor.getValue()) == '') {
            editor.clearGutter("error-gutter");
        } else {
            editor.getCommands();
            editor.clearGutter("error-gutter");
        }
    });
    editor.setSize("100%","100%");
    return editor;
}

function initializeEditor(htmlElement, mode){
    var editor = CodeMirror.fromTextArea(htmlElement, options);
    //Indicate that it must use the previously defined mode.
    options.mode = mode;
    //Parses the editor's text for alloy commands.
    editor.getCommands = function(){
        var runPattern = /(\W|^)run(\{|(\[\n\r\s]+\{)|([\n\r\s]+([^{\n\r\s]*)))/g;
        var checkPattern = /(\W|^)check(\{|(\[\n\r\s]+\{)|([\n\r\s]+([^{\n\r\s]*)))/g;
        var commands = [];
        var runNumber = 1;
        var checkNumber = 1;
        var input = this.getValue();

        var runMatches = runPattern.exec(input);
        while(runMatches != null){
            commands.push(runMatches[5]?runMatches[5]:"run$"+runNumber++);
            runMatches = runPattern.exec(input);
        }

        var checkMatches = checkPattern.exec(input);
        while(checkMatches != null){
            commands.push(checkMatches[5]?checkMatches[5]:"check$"+checkNumber++);
            checkMatches = checkPattern.exec(input);
        }
        //Adds the found commands to the session variables, triggering template events automatically.
        Session.set("commands",commands);
    };

    //editor.setSize("100%","100%");
    return editor;
}




