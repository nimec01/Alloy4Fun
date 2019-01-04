import classie from 'classie';
import 'qtip2/src/core.css';

import {
    clickGenUrl
} from "./genUrl"
import {
    zeroclipboard,
    getAnchorWithLink
} from "../../lib/editor/clipboard"


// Globals
/** @var instances The received instances */
instances = [];

/** @var instanceIndex The current instance index */
instanceIndex = 0;

/** @var maxInstanceNumber The number of instances in the variable instances */
maxInstanceNumber = -1;

/*Each template has a local dictionary of helpers that are made available to it, and this call specifies helpers to add to the template’s dictionary.*/
Template.alloyEditor.helpers({
    'drawInstance': function() {
        var instanceNumber = Session.get("currentInstance");
        if (instanceNumber == 0) $('#prev > button').prop('disabled', true);
        if (instanceNumber != undefined) {
            var instance = getCurrentInstance(instanceNumber);
            if (instance) {
                updateGraph(instance);
            }
        }
    },
    'getCommands': function() {
        var commands = Session.get("commands");
        if (commands && commands.length > 1) {
            $(".command-selection").show();
        } else $(".command-selection").hide();
        return commands;
    },
    'getTargetNode': function() {
        var target = Session.get("targetNode");
        if (target) return target.label;
    },
    'getType': function() {
        var target = Session.get("targetNode");
        if (target) return target.label.split("$")[0];
    },

});
Template.alloyEditor.events({
    'click #exec': function(evt) {
        currentlyProjectedTypes = [];
        currentFramePosition = {};
        allAtoms = [];
        atomPositions = {};
        $(".frame-navigation").hide();

        let commandLabel = getCommandLabel();
        if (commandLabel.length == 0) { //no command to run
            swal({
                title: "",
                text: "There are no commands to execute",
                icon: "warning",
                buttons: true,
                dangerMode: true,
            });
        } else { // Execute command
            let model = textEditor.getValue();
            Meteor.call('getInstances', model, 5, commandLabel, true, handleInterpretModelEvent);
        }
        // update button states after execution
        $("#exec > button").prop('disabled', true);
        $("#next > button").prop('disabled', false);
    },
    'change .command-selection > select': function() {
        $("#exec > button").prop('disabled', false);
    },
    'click #genUrl': clickGenUrl,
    'click #prev': function(evt) {
        if (evt.toElement.id != "prev") {
            if (!$("#prev > button").is(":disabled")) {
                let ni = getPreviousInstance();
                if (typeof ni !== 'undefined') {
                    updateGraph(ni);
                    if (instanceIndex == 0) {
                        $("#prev > button").prop('disabled', true);
                    }
                    $("#next > button").prop('disabled', false);
                }
            }
        }
    },
    'click #next': function(evt) {
        if (evt.toElement.id != "next") {
            if (!$("#next > button").is(":disabled")) {
                let ni = getNextInstance();
                if (typeof ni !== 'undefined') {
                    if (ni.unsat) {
                        $('#next > button').prop('disabled', true);
                        swal("No more satisfying instances!", "", "error");
                    } else {
                        console.log("requesting graph update");
                        updateGraph(ni);
                    }
                    if (instanceIndex + 1 == maxInstanceNumber) {
                        $("#next > button").prop('disabled', true);
                    }
                    $("#prev > button").prop('disabled', false);
                }
            }
        }
    },
    'click #genInstanceUrl': function() {
        var themeData = {
            atomSettings: atomSettings,
            relationSettings: relationSettings,
            generalSettings: generalSettings,
            currentFramePosition: currentFramePosition,
            currentlyProjectedTypes: currentlyProjectedTypes,
            metaPrimSigs: metaPrimSigs,
            metaSubsetSigs: metaSubsetSigs
        };
        console.log(instances);
        console.log(getCommandLabel());
        //obter o id do Run correspondente à instancia atual no browser
        var runID = Session.get("instances")[0 /*Session.get("currentInstance")*/ ].runID;

        Meteor.call("storeInstance", Session.get("last_id"), sat, getCommandLabel(), cy.json(), themeData, handleGenInstanceURLEvent)

        //Meteor.call('storeInstance', textEditor.getValue(), themeData, cy.json(), handleGenInstanceURLEvent);
        // Meteor.call('storeInstance', runID, themeData, cy.json(), handleGenInstanceURLEvent);
    },
    'click #validateModel': function() { // click on the validate button
        Meteor.call('validate', textEditor.getValue(), (err, res) => {
            if (err) console.error("Unable to connect to server")
            else {
                res = JSON.parse(res);
                if (!res.success) {
                    addErrorMarkerToGutter(res.errorMessage, res.errorLocation.line);
                    swal({
                        title: "There is at least an error on line " + res.errorLocation.line + "!",
                        text: "The corresponding line has been highlighted in the text area!",
                        type: "error"
                    });
                } else { // success
                    swal({
                        title: "The Model is Valid!",
                        text: "You're doing great!",
                        type: "success"
                    });
                }
            }
        })
    }
});
/*Callbacks added with this method are called once when an instance of Template.alloyEditor is rendered into DOM nodes and put into the document for the first time.*/
Template.alloyEditor.onRendered(function() {
    console.log("editor rendered");
    try {
        cy
    } catch (e) {
        initGraphViewer("instance");
    }
    //Adds click effects to Buttons
    buttonsEffects();
    //Hide Next, Previous, Run... buttons on startup
    hideButtons();

    //If there's subscribed data, process it.
    var model;
    if (Router.current().data)
        model = Router.current().data();
    console.log(model);

    if (model && textEditor) {
        var themeData;
        if (model.instance)
            themeData = model.instance.theme;
        //Place model on text editor
        var result = model.whole;
        textEditor.setValue(result);
        //Load theme settings;
        if (themeData) {
            atomSettings = themeData.atomSettings;
            relationSettings = themeData.relationSettings;
            generalSettings = themeData.generalSettings;
            currentFramePosition = themeData.currentFramePosition;
            currentlyProjectedTypes = themeData.currentlyProjectedTypes;
            if (themeData.metaPrimSigs) metaPrimSigs = themeData.metaPrimSigs;
            if (themeData.metaSubsetSigs) metaSubsetSigs = themeData.metaSubsetSigs;
        }
        //Load graph JSON data in case of instance sharing.
        if (model.instance && cy) {
            $('#instanceViewer').show();
            //cy.add(Router.current().data().instance.elements);
            cy.add(model.instance.graph.elements);
            updateElementSelectionContent();
        }
    }

    //Right click menu styling
    $(".command-selection").hide();
    (function($) {
        $(document).ready(function() {
            $('#cssmenu li.active').addClass('open').children('ul').show();
            $('#cssmenu li.has-sub>a').on('click', function() {
                $(this).removeAttr('href');
                var element = $(this).parent('li');
                if (element.hasClass('open')) {
                    element.removeClass('open');
                    element.find('li').removeClass('open');
                    element.find('ul').slideUp(200);
                } else {
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
});

/*------------- Server HANDLERS methods && Aux Functions ----------- */


/* Execbtn event handler
      result: getInstance(textEditor.getValue,..)*/
function handleInterpretModelEvent(err, result) {
    //TODO: insert model into database on getInstances
    console.log(result);
    $.unblockUI();
    $('#exec > button').prop('disabled', true);

    $('#url-permalink').empty() //remove previous links
    $("#genUrl > button").prop('disabled', false); // Restart shareModel option

    //clear projection combo box
    var select = document.getElementsByClassName("framePickerTarget");

    if (select)
        select = select[0];
    if (select) {
        var length = select.options.length;
        for (i = 0; i < length; i++) {
            select.remove(0);
        }
    }

    $('#instanceViewer').hide();
    $("#log").empty();
    var command = $('.command-selection > select option:selected').text();

    if (err) {
        //TODO: this is no longer applicable after no-soap
        if (err.error == 502) {
            swal("Syntax Error!", "", "error");
            var x = document.createElement("IMG");
            x.setAttribute("src", "/images/icons/error.png");
            x.setAttribute("width", "15");
            x.setAttribute("id", "error");
            x.setAttribute("title", err.reason.msg);
            textEditor.setGutterMarker(err.reason.line - 1, "error-gutter", x);
            textEditor.refresh();
            $('#next > button').prop('disabled', true);
            $('#prev > button').prop('disabled', true);
        }
    } else {
        storeInstances(result);
        if (Array.isArray(result))
            result = result[0];
        if (result.commandType && result.commandType == "check") {
            /* if the commandType == check */

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
        }

        if (result.unsat) {
            $('.empty-univ').fadeIn();
            $('#instanceViewer').hide();
            $("#genInstanceUrl").hide();
        }

        if (result.syntax_error) {
            swal("There is a syntax error!", "Please validate your model.", "error");
        }
    }
}

function storeInstances(allInstances) {
    instances = allInstances;
    instanceIndex = 0;
    maxInstanceNumber = allInstances.length;
}

function getNextInstance() {
    return instances[++instanceIndex];
}

function getPreviousInstance() {
    return instances[--instanceIndex];
}

function getCommandLabel() {
    return Session.get("commands").length > 1 ? $('.command-selection > select option:selected').text() : Session.get("commands")[0];
}

/* geninstanceurlbtn event handler after storeInstance method */
function handleGenInstanceURLEvent(err, result) {
    if (!err) {
        // if the URL was generated successfully, create and append a new element to the HTML containing it.
        var url = getAnchorWithLink(result, "instance link");

        var clipboard = document.createElement('div');
        clipboard.className = "col-lg-12 col-md-12 col-sm-12 col-xs-12";
        clipboard.innerHTML = "<button class='clipboardbutton cbutton cbutton--effect-boris'><img src='/images/icons/clipboard.png' /><i class='legend'>copy to clipboard</i></button></div>";


        var textcenter = document.createElement('div');
        textcenter.className = "text-center";
        textcenter.id = "instance_permalink";
        textcenter.appendChild(url);
        textcenter.appendChild(clipboard);

        $("#url-instance-permalink").empty()
        document.getElementById('url-instance-permalink').appendChild(textcenter);
        $("#genInstanceUrl > button").prop('disabled', true);
        zeroclipboard();
    }
}

getCurrentInstance = function() {
    return instances[instanceIndex]
};

/*onRendered aux functions*/
function buttonsEffects() {

    function mobilecheck() {
        var check = false;
        (function(a) {
            if (/(android|ipad|playbook|silk|bb\d+|meego).+mobile|avantgo|bada\/|blackberry|blazer|compal|elaine|fennec|hiptop|iemobile|ip(hone|od)|iris|kindle|lge |maemo|midp|mmp|netfront|opera m(ob|in)i|palm( os)?|phone|p(ixi|re)\/|plucker|pocket|psp|series(4|6)0|symbian|treo|up\.(browser|link)|vodafone|wap|windows (ce|phone)|xda|xiino/i.test(a) || /1207|6310|6590|3gso|4thp|50[1-6]i|770s|802s|a wa|abac|ac(er|oo|s\-)|ai(ko|rn)|al(av|ca|co)|amoi|an(ex|ny|yw)|aptu|ar(ch|go)|as(te|us)|attw|au(di|\-m|r |s )|avan|be(ck|ll|nq)|bi(lb|rd)|bl(ac|az)|br(e|v)w|bumb|bw\-(n|u)|c55\/|capi|ccwa|cdm\-|cell|chtm|cldc|cmd\-|co(mp|nd)|craw|da(it|ll|ng)|dbte|dc\-s|devi|dica|dmob|do(c|p)o|ds(12|\-d)|el(49|ai)|em(l2|ul)|er(ic|k0)|esl8|ez([4-7]0|os|wa|ze)|fetc|fly(\-|_)|g1 u|g560|gene|gf\-5|g\-mo|go(\.w|od)|gr(ad|un)|haie|hcit|hd\-(m|p|t)|hei\-|hi(pt|ta)|hp( i|ip)|hs\-c|ht(c(\-| |_|a|g|p|s|t)|tp)|hu(aw|tc)|i\-(20|go|ma)|i230|iac( |\-|\/)|ibro|idea|ig01|ikom|im1k|inno|ipaq|iris|ja(t|v)a|jbro|jemu|jigs|kddi|keji|kgt( |\/)|klon|kpt |kwc\-|kyo(c|k)|le(no|xi)|lg( g|\/(k|l|u)|50|54|\-[a-w])|libw|lynx|m1\-w|m3ga|m50\/|ma(te|ui|xo)|mc(01|21|ca)|m\-cr|me(rc|ri)|mi(o8|oa|ts)|mmef|mo(01|02|bi|de|do|t(\-| |o|v)|zz)|mt(50|p1|v )|mwbp|mywa|n10[0-2]|n20[2-3]|n30(0|2)|n50(0|2|5)|n7(0(0|1)|10)|ne((c|m)\-|on|tf|wf|wg|wt)|nok(6|i)|nzph|o2im|op(ti|wv)|oran|owg1|p800|pan(a|d|t)|pdxg|pg(13|\-([1-8]|c))|phil|pire|pl(ay|uc)|pn\-2|po(ck|rt|se)|prox|psio|pt\-g|qa\-a|qc(07|12|21|32|60|\-[2-7]|i\-)|qtek|r380|r600|raks|rim9|ro(ve|zo)|s55\/|sa(ge|ma|mm|ms|ny|va)|sc(01|h\-|oo|p\-)|sdk\/|se(c(\-|0|1)|47|mc|nd|ri)|sgh\-|shar|sie(\-|m)|sk\-0|sl(45|id)|sm(al|ar|b3|it|t5)|so(ft|ny)|sp(01|h\-|v\-|v )|sy(01|mb)|t2(18|50)|t6(00|10|18)|ta(gt|lk)|tcl\-|tdg\-|tel(i|m)|tim\-|t\-mo|to(pl|sh)|ts(70|m\-|m3|m5)|tx\-9|up(\.b|g1|si)|utst|v400|v750|veri|vi(rg|te)|vk(40|5[0-3]|\-v)|vm40|voda|vulc|vx(52|53|60|61|70|80|81|83|85|98)|w3c(\-| )|webc|whit|wi(g |nc|nw)|wmlb|wonu|x700|yas\-|your|zeto|zte\-/i.test(a.substr(0, 4))) check = true
        })(navigator.userAgent || navigator.vendor || window.opera);
        return check;
    }

    var support = {
            animations: Modernizr.cssanimations
        },
        animEndEventNames = {
            'WebkitAnimation': 'webkitAnimationEnd',
            'OAnimation': 'oAnimationEnd',
            'msAnimation': 'MSAnimationEnd',
            'animation': 'animationend'
        },
        animEndEventName = animEndEventNames[Modernizr.prefixed('animation')],
        onEndAnimation = function(el, callback) {
            var onEndCallbackFn = function(ev) {
                if (support.animations) {
                    if (ev.target != this) return;
                    this.removeEventListener(animEndEventName, onEndCallbackFn);
                }
                if (callback && typeof callback === 'function') {
                    callback.call();
                }
            };
            if (support.animations) {
                el.addEventListener(animEndEventName, onEndCallbackFn);
            } else {
                onEndCallbackFn();
            }
        },
        eventtype = mobilecheck() ? 'touchstart' : 'click';

    [].slice.call(document.querySelectorAll('.cbutton')).forEach(function(el) {
        el.addEventListener(eventtype, function() {
            classie.add(el, 'cbutton--click');
            onEndAnimation(classie.has(el, 'cbutton--complex') ? el.querySelector('.cbutton__helper') : el, function() {
                classie.remove(el, 'cbutton--click');
            });
        });
    });
}

function hideButtons() {
    $('#exec > button').prop('disabled', true);
    $('#next > button').prop('disabled', true);
    $('#prev > button').prop('disabled', true);
    $('#validateModel > button').prop('disabled', true);
    $('.permalink > button').prop('disabled', true);
}