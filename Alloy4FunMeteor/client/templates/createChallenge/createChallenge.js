/**
 * Created by josep on 31/03/2016.
 */

import {initializeAlloyCreateChallengeEditor} from '/imports/editor/EditorInitializer';

import 'qtip2';

Template.createChallenge.helpers({
  /*Edit challenge  */
    'loadContent' : function(){
        if(this.whole && this.password && challengeEditor){
            challengeEditor.setValue(this.whole);
            setLockedLines(Router.current().data().lockedLines);
            $("#challengePassword")[0].value = this.password;
        }
    },
  /*Display challenge statistics*/
    'displayStatistics' : function(){
        var statistics = Session.get("statistics");
        if(statistics) return statistics;
        else return [];
    }
});

Template.createChallenge.events({

    /* Save and Share Challenge */
    "click #saveAndShare" : function (){
        /* Disable save and Share button */
        $('#saveAndShare').prop('disabled', true);

        /* Clear errors from editor's gutter */
        challengeEditor.clearGutter("error-gutter");

        /* #challengePassword -> input id*/
        var password = $("#challengePassword")[0].value;

        /* if password length > 0 */
        if(password!="" ){
            /* Execute getInstance mainly because a model verification is needed, handleInterpretModelEvent alert model errors and store the challenge*/
            Meteor.call('getInstance', challengeEditor.getValue()+'\nrun test{}', Meteor.default_connection._lastSessionId, 0, 'test' , true, handleInterpretModelEvent);
        }
    },
    /* Password requirements */
    "input #challengePassword" : function () {
        $("#challengePassword").removeClass("missing-field");
        $(".lock-password").removeClass("wrong");
    }

});



/*----- ---Save and Share Event ------- */

/* saveAndShare event handler*/
function handleInterpretModelEvent(err, result){
    if (err) {
        if (err.error == 502) {
            //Syntax error, add error marker to editor's gutter
            addErrorMarkerToGutter(err.reason.msg, err.reason.line);
            //Warn user
            swal({
                    title: "Challenge contains syntax errors",
                    text: "Are you sure you want to share it?",
                    type: "warning",
                    showCancelButton: true,
                    confirmButtonColor: "#DD6B55",
                    confirmButtonText: "Yes, share it!",
                    closeOnConfirm: true
                },
                //Action in user decides to share without removing syntax errors
                storeChallenge);
        }
    }else{
        storeChallenge();
    }
}

/* Method that stores a challenge, uses 'storeChallenge' server method and handleResponse with the result
   checkSecretBlocks function used before storeChallenge
*/
function storeChallenge(){
    var error = false;
    try{
        checkSecretBlocks();
    }catch(err){
        error = true;
        switch(err.number){
            case 1:
                swal("Error",err.message,"error");
                break;
            case 2:
                addErrorMarkerToGutter(err.message, err.lineNumber);
                swal("Error",err.message,"error");
                break;
        }
    }
    if(!error)Meteor.call('storeChallenge',challengeEditor.getValue(), $("#challengePassword")[0].value, true, 'original', getLockedMarkers(), handleResponse);
}

/* Handle the result from storeChallenge method, displays the generated permalink */
function handleResponse(err, result){
    if(err){
        if(err.error == 503) {
            var x = document.createElement("IMG");
            x.setAttribute("src", "/images/icons/error.png");
            x.setAttribute("width", "15");
            x.setAttribute("id", "error");
            x.setAttribute("title", "Missing check name");
            challengeEditor.setGutterMarker(parseInt(err.reason) - 1, "error-gutter", x);
            $('#error').qtip({
                // your options
                show: '',
                hide: {
                    event: 'unfocus'
                },
                content: {
                    prerender: true, // important
                    text: 'Please name this check command.'
                }
            }).qtip('show');
        }else if(err.error ==504){
            //TODO: handle error
        }
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
        zeroclipboard();
    }
}

function checkSecretBlocks(){
    var challenge= challengeEditor.getValue();

    var secretsStart = getIndexesOf(/\/\/START_SECRET/gi, challenge);
    var secretsEnd = getIndexesOf(/\/\/END_SECRET/gi, challenge);

    if(secretsStart.length != secretsEnd.length){
        throw {number : 1, message : "Different number of SECRET open and closing tags! (//START_SECRET .. //END_SECRET)"};
    }

    while(secretsStart.length>0){
        var secretStart = secretsStart.shift();
        var secretEnd = secretsEnd.shift();
        if(secretStart > secretEnd) {
            throw {number : 2,lineNumber : challengeEditor.posFromIndex(secretEnd).line+1, message : "END tag before any START ! (//START_SECRET .. //END_SECRET)"};
        }
    }

}

/*------- ------ ------ ----- ----- ---- */



/*
Callbacks added with this method are called once when an instance of Template.createChallenge is rendered into DOM nodes and put into the document for the first time.
*/
Template.createChallenge.onRendered(function () {
    challengeEditor = initializeAlloyCreateChallengeEditor(document.getElementById("challengeEditor"));
    $('#saveAndShare').qtip({
        content: {
            text: 'Please fill in the password field to protect the secrets.',
        },
        position: {
            target: 'mouse', // Position at the mouse...
            adjust: { mouse: false } // ...but don't follow it!
        },
        hide: {
            event: 'unfocus'
        },
        show: 'mousedown',
        events : {
            show: function (event) {
                var password = $("#challengePassword")[0].value;
                if (password != "") event.preventDefault();
                else{
                    $('#saveAndShare').data('qtip').options.content.text = 'Please fill in the password field to protect the secrets.';
                    $("#challengePassword").toggleClass("missing-field");
                    $(".lock-password").toggleClass("wrong");
                }

            }
        }
    });
    $('#saveAndShare').prop('disabled', true);
    var password = Session.get("password");
    if(Router.current().data && Router.current().data()._id){
        Meteor.subscribe('editChallenge', Router.current().data()._id, password);
        Meteor.call('getStatistics', Router.current().data()._id, handleGetStatistics);
    }
});

function handleGetStatistics(err, result){
    if(err){
        //TODO: Handle statistics error
    }else{
        Session.set("statistics", result);
    }
}

function zeroclipboard() {
    var client = new ZeroClipboard($(".clipboardbutton"));
    client.on("copy", function (event) {
        var clipboard = event.clipboardData;
        clipboard.setData("text/plain", $(".urlinfo").html());
    });
}
