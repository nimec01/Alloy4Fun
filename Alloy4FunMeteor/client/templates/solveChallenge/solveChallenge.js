/**
 * Created by josep on 06/04/2016.
 */
import {initializeAlloySolveChallengeEditor} from '/imports/editor/EditorInitializer';
import 'qtip2';

Template.solveChallenge.helpers({
    'userCommands' : function(){
        var commands = Session.get("commands");
        return commands;
    },
    'getTargetNode' : function (){
        var target = Session.get("targetNode");
        if (target) return target.label;
    },
    'getType' : function (){
        var target = Session.get("targetNode");
        if (target) return target.label.split("$")[0];
    },
    'isSolution' : function (){
        return !Router.current().data().solution;
    }
});


Template.solveChallenge.events({
    "click #execbtn" : function (){
        //Check if model was changed since last execution
        var history = Session.get("modelHistory");
        var model = challengeEditor.getValue();
        Session.set("currentInstance",0);
        blockInterface();
        $("#instancenav").show();
        $("#next > button").prop('disabled', false);
        $("#prev > button").prop('disabled', true);
        Meteor.call('assertChallenge', model, Router.current().data()._id, $('.command-selection > select option:selected').text(), Meteor.default_connection._lastSessionId, Session.get("currentInstance"), true, history.changed, handleResponse);


    },
    "click #next" : function(){
        $("#prev > button").prop('disabled', false);
        blockInterface();
        var model = challengeEditor.getValue();
        Session.set("currentInstance",Session.get("currentInstance")+1);
        Meteor.call('assertChallenge', model, Router.current().data()._id, $('.command-selection > select option:selected').text(), Meteor.default_connection._lastSessionId,Session.get("currentInstance"), false, false, handleResponse);
    },
    "click #prev" : function(){
        $("#next > button").prop('disabled', false);
        var model = challengeEditor.getValue();
        blockInterface();
        Session.set("currentInstance",Session.get("currentInstance")-1);
        if(Session.get("currentInstance")==0)$("#prev > button").prop('disabled', true);
        Meteor.call('assertChallenge', model, Router.current().data()._id, $('.command-selection > select option:selected').text(), Meteor.default_connection._lastSessionId,Session.get("currentInstance"), false, false, handleResponse);
    },
    "input #challengePassword" : function () {

    },
    'change .shapePickerTarget' : function (evt) {
        if (evt.target.value != "disabled") {
            var target = Session.get("targetNode");
            cy.nodes("[id='" + target.id + "']").css({'shape': evt.target.value});
            evt.target.value = "disabled";
        }
    },
    'change .shapePickerType' : function (evt){
        if(evt.target.value!="disabled") {
            var target = Session.get("targetNode");
            cy.nodes("[type='" + target.type + "']").css({'shape': evt.target.value});
            evt.target.value = "disabled";
        }
    },
    'click #editChallenge' : function (){
        var password = $("#challengePassword")[0].value;
        Meteor.call('unlockChallenge', Router.current().data()._id, password, handleUnlockChallenge);
    },
    'click #shareSolution' : function(){
        $.blockUI({message: '<div class="sk-circle"><div class="sk-circle1 sk-child"></div><div class="sk-circle2 sk-child"></div><div class="sk-circle3 sk-child"></div><div class="sk-circle4 sk-child"></div><div class="sk-circle5 sk-child"></div><div class="sk-circle6 sk-child"></div><div class="sk-circle7 sk-child"></div><div class="sk-circle8 sk-child"></div><div class="sk-circle9 sk-child"></div><div class="sk-circle10 sk-child"></div><div class="sk-circle11 sk-child"></div><div class="sk-circle12 sk-child"></div></div>',
            css: { border: '0px' , background:  'transparent'}});
        $("#shareSolution").prop('disabled', true);
        var history = Session.get("modelHistory");

        Meteor.call('storeSolution', challengeEditor.getValue(), history.id, lockedMarkers(), handleStoreSolution);
    },


});

//Returns lines marked with a lock
function lockedMarkers(){
    var i = 0, line;
    var lockedLines = [];
    while(line = challengeEditor.lineInfo(i++)){
        if(line.gutterMarkers && line.gutterMarkers.breakpoints)lockedLines.push(i);
    }
    return lockedLines;
}

function handleStoreSolution(err, result){

    if(err){
        //TODO: Handle Error
    }else{
        var url = document.createElement('div');
        url.className = "col-lg-12 col-md-12 col-sm-12 col-xs-12";
        var anchor = document.createElement('a');
        anchor.href = "/solveChallenge/" +  result;
        anchor.className= "urlinfo";
        anchor.innerHTML =  window.location.origin +"/solveChallenge/" +  result;
        url.appendChild(anchor);

        var clipboard = document.createElement('div');
        clipboard.className = "col-lg-12 col-md-12 col-sm-12 col-xs-12";
        clipboard.innerHTML = "<button class='clipboardbutton cbutton cbutton--effect-boris'><img src='/images/icons/clipboard.png' /><i class='legend'>copy to clipboard</i></button></div>";

        var textcenter = document.createElement('div');
        textcenter.className = "text-center";
        textcenter.id = "permalink";
        textcenter.appendChild(url);
        textcenter.appendChild(clipboard);

        document.getElementById('url-challenge-permalink').appendChild(textcenter);
    }
    unblockInterface();
}

function handleUnlockChallenge (err, result){
    unblockInterface();
    if (err){
        if(err.error ==506){
            $('#challengePassword').qtip({
                // your options
                show: '',
                hide: {
                    event: 'unfocus'
                },
                position: {
                    my: 'top left',  // Position my top left...
                    at: 'bottom left', // at the bottom right of...
                    target: $('#challengePassword') // my target
                },
                content: {
                    prerender: true, // important
                    text: 'Invalid Password.'
                }
            }).qtip('show');
        }
    }else {
        var password = $("#challengePassword")[0].value;
        Session.set('password', password);
        var id= Router.current().data()._id;
        Router.go('editChallenge', {_id: id} );
    }
}

function handleResponse(err, result){
    $.unblockUI();
    $('#instanceViewer').hide();
    $("#log").empty();
    var command = $('.command-selection > select option:selected').text();
    if(err){
        if(err.error == 502) {
            swal("Syntax Error!", "", "error");
            var x = document.createElement("IMG");
            x.setAttribute("src", "/images/icons/error.png");
            x.setAttribute("width", "15");
            x.setAttribute("id", "error");
            x.setAttribute("title", err.reason.msg);
            challengeEditor.setGutterMarker(err.reason.line - 1, "error-gutter", x);
            challengeEditor.refresh();
            $('#next > button').prop('disabled', true);
            $('#prev > button').prop('disabled', true);
        }
    }else{
        updateInstances(result);
        $("#instancenav").show();
        var history = Session.get("modelHistory");

        //Update history
        if (history.changed) {
            history.changed = false;
            Meteor.call("storeDerivation", challengeEditor.getValue(), history.id, !result.unsat, handleStoreDerivation);
        }
        console.log(result);
        if(result.commandType && result.commandType == "check") {
            var log = document.createElement('div');
            log.className = "col-lg-12 col-md-12 col-sm-12 col-xs-12";
            var paragraph = document.createElement('p');

            if (result.unsat) {
                $('#instancenav').hide();
                paragraph.innerHTML = "No counter-examples. " + command + " solved!";
                paragraph.className = "log-complete";
            } else {
                paragraph.innerHTML = "Invalid solution, checking " + command + " revealed a counter-example.";
                paragraph.className = "log-wrong";
                updateGraph(result);
            }
        
            log.appendChild(paragraph);
            $("#log")[0].appendChild(log);
        }else{
            updateGraph(result);
        }

    }
}

function handleStoreDerivation(err, result){
    Session.set("modelHistory", {id : result, changed : false});
    unblockInterface();
}

Template.solveChallenge.onRendered(function () {
    blockInterface();
    Session.set("commands",undefined);

    //Initialize text editor
    challengeEditor = initializeAlloySolveChallengeEditor(document.getElementById("challengeEditor"));
    //Initialize instance viewer
    initGraphViewer("instance");
    //Fill editor with the id referent model
    challengeEditor.setValue(Router.current().data().cut);
    Session.set("modelHistory", {id: Router.current().data()._id, changed: false});

    //Instance viewer right click settings customization
    (function($){
        $(document).ready(function(){

            $('#cssmenu li.active').addClass('open').children('ul').show();
            $('#cssmenu li.has-sub>a').on('click', function(){
                $(this).removeAttr('href');
                var element = $(this).parent('li');
                if (element.hasClass('open')) {
                    element.removeClass('open');
                    element.find('li').removeClass('open');
                    element.find('ul').slideUp(200);
                }
                else {
                    element.addClass('open');
                    element.children('ul').slideDown(200);
                    element.siblings('li').children('ul').slideUp(200);
                    element.siblings('li').removeClass('open');
                    element.siblings('li').find('li').removeClass('open');
                    element.siblings('li').find('ul').slideUp(200);
                }
            });

        });
    })(jQuery);
    $('#optionsMenu').hide();

    lockLines(Router.current().data().lockedLines);
    unblockInterface();
});

function lockLines(lockedLines){
    lockedLines.forEach(function(n){
        var info = challengeEditor.lineInfo(n);
        challengeEditor.setGutterMarker(n-1, "breakpoints", info.gutterMarkers ? null : makeMarker());
        challengeEditor.markText({line : n-1, ch: 0},{line: n , ch : 0}, { className: "challenge-lock", readOnly: true, inclusiveLeft: true, clearWhenEmpty:false});
    });
}

blockInterface = function(){
    $.blockUI({message: '<div class="sk-circle"><div class="sk-circle1 sk-child"></div><div class="sk-circle2 sk-child"></div><div class="sk-circle3 sk-child"></div><div class="sk-circle4 sk-child"></div><div class="sk-circle5 sk-child"></div><div class="sk-circle6 sk-child"></div><div class="sk-circle7 sk-child"></div><div class="sk-circle8 sk-child"></div><div class="sk-circle9 sk-child"></div><div class="sk-circle10 sk-child"></div><div class="sk-circle11 sk-child"></div><div class="sk-circle12 sk-child"></div></div>',
            css: { border: '0px' , background:  'transparent'}});
};

unblockInterface = function(){
    $.unblockUI();
};
