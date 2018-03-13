/**
 * Created by josep on 31/03/2016.
 */
import 'qtip2';

Template.createChallengeWrapper.helpers({
    'unlocked' : function(){
        var password = Session.get("password");
        return Router.current().data!=undefined && !password;
    }
});


Template.createChallengeWrapper.events({
    'click #editChallenge' : function (){
        var password = $("#unlockChallengePassword")[0].value;
        Meteor.call('unlockChallenge', Router.current().data()._id, password, handleUnlockChallenge);
    }

});

function handleUnlockChallenge (err, result){
    if (err){
        if(err.error ==506){
            $('#unlockChallengePassword').qtip({
                // your options
                show: '',
                hide: {
                    event: 'unfocus'
                },
                position: {
                    my: 'top left',  // Position my top left...
                    at: 'bottom left', // at the bottom right of...
                    target: $('#unlockChallengePassword') // my target
                },
                content: {
                    prerender: true, // important
                    text: 'Invalid Password.'
                }
            }).qtip('show');
        }
    }else {
        var password = $("#unlockChallengePassword")[0].value;
        Session.set("password",password);
    }
}

Template.createChallengeWrapper.onRendered(function () {
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
                if (password != "") {
                    event.preventDefault();
                }
                else{
                    $('#saveAndShare').data('qtip').options.content.text = 'Please fill in the password field to protect the secrets.';
                    $("#challengePassword").toggleClass("missing-field");
                    $(".lock-password").toggleClass("wrong");
                }

            }
        }
    });
});

