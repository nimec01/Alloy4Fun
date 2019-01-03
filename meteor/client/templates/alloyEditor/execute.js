export {
    clickExecute
}

/**
 * Function to handle click on "Execute" button
 * @param {Event} evt 
 */
function clickExecute(evt) {
    currentlyProjectedTypes = [];
    currentFramePosition = {};
    allAtoms = [];
    atomPositions = {};
    $(".frame-navigation").hide();
    if (evt.toElement.id != "exec")

        if (!$("#exec > button").is(":disabled")) { /* if the button is available, check if there are commands to execute*/
            var commandLabel = Session.get("commands").length > 1 ? $('.command-selection > select option:selected').text() : Session.get("commands");
            if (commandLabel.length == 0) {
                swal({
                    title: "",
                    text: "There are no commands to execute",
                    icon: "warning",
                    buttons: true,
                    dangerMode: true,
                });

            } else { /* Execute command */

                /*//LOCKED insertion */
                var modelToShare = "";
                var i = 0,
                    line, inLockBlock = false;
                var braces;
                var foundbraces = false;
                while (line = textEditor.lineInfo(i++)) {
                    if (line.gutterMarkers && line.gutterMarkers.breakpoints) {
                        if (!inLockBlock) {
                            modelToShare += "\n//LOCKED";
                            inLockBlock = true;
                            foundbraces = false;
                            braces = 0;
                        }
                        if (inLockBlock) {
                            for (c = 0; c < line.text.length; c++) {
                                switch (line.text.charAt(c)) {
                                    case '{':
                                        braces++;
                                        foundbraces = true;
                                        break;
                                    case '}':
                                        braces--;
                                        break;
                                }
                            }
                        }
                    } else {
                        inLockBlock = false;
                        foundbraces = false;
                    }
                    modelToShare += "\n" + line.text;

                    if (foundbraces && braces == 0) {
                        inLockBlock = false;
                        modelToShare += "\n";
                    }
                }

                var secrets = "";
                if (!(id = Router.current().params._id)) {
                    id = "Original";
                }
                if ((id != "Original") && Router.current().data().secrets) secrets = Router.current().data().secrets;
                Meteor.call('getInstance', (modelToShare /*textEditor.getValue()*/ + secrets), Meteor.default_connection._lastSessionId, 0, commandLabel, true, id, Session.get("last_id"), handleInterpretModelEvent);
            }
            $("#exec > button").prop('disabled', true); /* available buttons */

            $("#next > button").prop('disabled', false);
        }

}