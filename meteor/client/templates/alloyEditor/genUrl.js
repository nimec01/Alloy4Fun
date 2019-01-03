import {
    zeroclipboard, getAnchorWithLink
} from "../../lib/editor/clipboard"

export {
    clickGenUrl
};

function clickGenUrl(evt) {
	console.log(evt);
    if (evt.toElement.id != "genUrl") {
        var themeData = {
            atomSettings: atomSettings,
            relationSettings: relationSettings,
            generalSettings: generalSettings,
            currentFramePosition: currentFramePosition,
            currentlyProjectedTypes: currentlyProjectedTypes
        };
        if (!$("#genUrl > button").is(":disabled")) { //if button is not disabled
            var modelToShare = textEditor.getValue();

            if (id = Router.current().params._id) { //if its loaded through an URL its a derivationOf model
                //so acontece num link publico
                //handle SECRETs
                if ((secrets = Router.current().data().secrets) && containsValidSecret(modelToShare)) {
                    swal({
                        title: "This model contains information that cannot be shared!",
                        text: "Are you sure you want to share it?",
                        type: "warning",
                        showCancelButton: true,
                        confirmButtonColor: "#DD6B55",
                        confirmButtonText: "Yes, share it!",
                        closeOnConfirm: true
                    }, function() {
                        Meteor.call('genURL', modelToShare, "Original", false, Session.get("last_id"), themeData, handleGenURLEvent);
                    });
                } else {
                    if (secrets.length == 0) {
                        //se tiver um ou mais valid secret com run check e assert anonimos, pergunta
                        if (containsValidSecretWithAnonymousCommand(modelToShare)) {
                            swal({
                                title: "This model contains an anonymous Command!",
                                text: "Are you sure you want to share it?",
                                type: "warning",
                                showCancelButton: true,
                                confirmButtonColor: "#DD6B55",
                                confirmButtonText: "Yes, share it!",
                                closeOnConfirm: true
                            }, function() {
                                Meteor.call('genURL', modelToShare, id, false, Session.get("last_id"), themeData, handleGenURLEvent);
                            });
                        } else
                            Meteor.call('genURL', modelToShare, id, false, Session.get("last_id"), themeData, handleGenURLEvent);
                    } else
                        Meteor.call('genURL', modelToShare + secrets, id, true, Session.get("last_id"), themeData, handleGenURLEvent)
                }
            } else { // Otherwise this a new model (not based in any other)
                if (containsValidSecretWithAnonymousCommand(modelToShare)) {
                    swal({
                        title: "This model contains an anonymous Command!",
                        text: "Are you sure you want to share it?",
                        type: "warning",
                        showCancelButton: true,
                        confirmButtonColor: "#DD6B55",
                        confirmButtonText: "Yes, share it!",
                        closeOnConfirm: true
                    }, function() {
                        Meteor.call('genURL', modelToShare, "Original", false, Session.get("last_id"), themeData, handleGenURLEvent);
                    });
                } else
                    Meteor.call('genURL', modelToShare, "Original", false, Session.get("last_id"), themeData, handleGenURLEvent);
            }
        }
    }
}


function containsValidSecretWithAnonymousCommand(model) {
    var lastSecret = 0;
    while ((i = model.indexOf("//SECRET\n", lastSecret)) >= 0) {
        var s = model.substr(i + "//SECRET\n".length).trim();
        //se o resto do texto comecar com a expressao abaixo entao contem
        //um comando anonimo
        if (s.match("^(assert|run|check)([ \t\n])*[{]")) {
            return true;
        }

        lastSecret = i + 1;
    }
    return false;
}


/* genUrlbtn event handler after genUrl method */
function handleGenURLEvent(err, result) {
    if (!err) {
        // if the URL was generated successfully, create and append a new element to the HTML containing it.
        var url = getAnchorWithLink(result['public'], "public link");
		var urlPrivate = getAnchorWithLink(result['private'], "private link");
		console.log(url, urlPrivate);

        var textcenter = document.createElement('div');
        textcenter.className = "text-center";
        textcenter.id = "permalink";
		textcenter.appendChild(url);
		textcenter.appendChild(urlPrivate);

        document.getElementById('url-permalink').appendChild(textcenter);
        $("#genUrl > button").prop('disabled', true);
        zeroclipboard();

        if (result.last_id) {
            Session.set("last_id", result.last_id);
        }
    }
}
