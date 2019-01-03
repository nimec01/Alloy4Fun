import classie from 'classie';
import 'qtip2/src/core.css';
import { isParagraph } from '../../../lib/editor/text';


/* Each template has a local dictionary of helpers that are made available to it, and this call specifies helpers to add to the template’s dictionary. */
Template.alloyEditor.helpers({
    drawInstance() {
        const instanceNumber = Session.get('currentInstance');
        if (instanceNumber == 0) $('#prev > button').prop('disabled', true);
        if (instanceNumber != undefined) {
            const instance = getCurrentInstance(instanceNumber);
            if (instance) {
                updateGraph(instance);
            }
        }
    },
    getCommands() {
        const commands = Session.get('commands');
        if (commands && commands.length > 1) {
            $('.command-selection').show();
        } else $('.command-selection').hide();
        return commands;
    },
    getTargetNode() {
        const target = Session.get('targetNode');
        if (target) return target.label;
    },
    getType() {
        const target = Session.get('targetNode');
        if (target) return target.label.split('$')[0];
    },

});
Template.alloyEditor.events({
    'click #exec'(evt) { // click on "Execute" button
        currentlyProjectedTypes = [];
        currentFramePosition = {};
        allAtoms = [];
        atomPositions = {};
        $('.frame-navigation').hide();
        if (evt.toElement.id != 'exec') {
            if (!$('#exec > button').is(':disabled')) { /* if the button is available, check if there are commands to execute */
                const commandLabel = Session.get('commands').length > 1 ? $('.command-selection > select option:selected').text() : Session.get('commands');
                if (commandLabel.length == 0) {
                    swal({
                        title: '',
                        text: 'There are no commands to execute',
                        icon: 'warning',
                        buttons: true,
                        dangerMode: true,
                    });
                } else { /* Execute command */
                    /* //LOCKED insertion */
                    let modelToShare = '';
                    let i = 0;


                    let line; let
                        inLockBlock = false;
                    let braces;
                    let foundbraces = false;
                    while (line = textEditor.lineInfo(i++)) {
                        if (line.gutterMarkers && line.gutterMarkers.breakpoints) {
                            if (!inLockBlock) {
                                modelToShare += '\n//LOCKED';
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
                        modelToShare += `\n${line.text}`;

                        if (foundbraces && braces == 0) {
                            inLockBlock = false;
                            modelToShare += '\n';
                        }
                    }

                    let secrets = '';
                    if (!(id = Router.current().params._id)) {
                        id = 'Original';
                    }
                    if ((id != 'Original') && Router.current().data().secrets) secrets = Router.current().data().secrets;
                    Meteor.call('getInstance', (modelToShare /* textEditor.getValue() */ + secrets), Meteor.default_connection._lastSessionId, 0, commandLabel, true, id, Session.get('last_id'), handleInterpretModelEvent);
                }
                $('#exec > button').prop('disabled', true); /* available buttons */

                $('#next > button').prop('disabled', false);
            }
        }
    },
    'change .command-selection > select'() {
        $('#exec > button').prop('disabled', false);
    },
    'click #genUrl'(evt) {
        if (evt.toElement.id != 'genUrl') {
            const themeData = {
                atomSettings,
                relationSettings,
                generalSettings,
                currentFramePosition,
                currentlyProjectedTypes,
            };
            if (!$('#genUrl > button').is(':disabled')) {
                /* //LOCKED insertion */
                let modelToShare = '';
                let i = 0;


                let line; let
                    inLockBlock = false;
                let braces;
                let foundbraces = false;
                while (line = textEditor.lineInfo(i++)) {
                    if (line.gutterMarkers && line.gutterMarkers.breakpoints) {
                        if (!inLockBlock) {
                            modelToShare += '\n//LOCKED';
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
                    modelToShare += `\n${line.text}`;
                    if (foundbraces && braces == 0) {
                        inLockBlock = false;
                        modelToShare += '\n';
                    }
                }

                if (id = Router.current().params._id) { /* if its loaded through an URL its a derivationOf model */
                    // so acontece num link publico
                    if ((secrets = Router.current().data().secrets) && containsValidSecret(modelToShare)) {
                        swal({
                            title: 'This model contains information that cannot be shared!',
                            text: 'Are you sure you want to share it?',
                            type: 'warning',
                            showCancelButton: true,
                            confirmButtonColor: '#DD6B55',
                            confirmButtonText: 'Yes, share it!',
                            closeOnConfirm: true,
                        }, () => {
                            Meteor.call('genURL', modelToShare, 'Original', false, Session.get('last_id'), themeData, handleGenURLEvent);
                        });
                    } else if (secrets.length == 0) {
                        // se tiver um ou mais valid secret com run check e assert anonimos, pergunta
                        if (containsValidSecretWithAnonymousCommand(modelToShare)) {
                            swal({
                                title: 'This model contains an anonymous Command!',
                                text: 'Are you sure you want to share it?',
                                type: 'warning',
                                showCancelButton: true,
                                confirmButtonColor: '#DD6B55',
                                confirmButtonText: 'Yes, share it!',
                                closeOnConfirm: true,
                            }, () => {
                                Meteor.call('genURL', modelToShare, id, false, Session.get('last_id'), themeData, handleGenURLEvent);
                            });
                        } else Meteor.call('genURL', modelToShare, id, false, Session.get('last_id'), themeData, handleGenURLEvent);
                    } else Meteor.call('genURL', modelToShare + secrets, id, true, Session.get('last_id'), themeData, handleGenURLEvent);
                } else { /* Otherwise its an original model */
                    if (containsValidSecretWithAnonymousCommand(modelToShare)) {
                        swal({
                            title: 'This model contains an anonymous Command!',
                            text: 'Are you sure you want to share it?',
                            type: 'warning',
                            showCancelButton: true,
                            confirmButtonColor: '#DD6B55',
                            confirmButtonText: 'Yes, share it!',
                            closeOnConfirm: true,
                        }, () => {
                            Meteor.call('genURL', modelToShare, 'Original', false, Session.get('last_id'), themeData, handleGenURLEvent);
                        });
                    } else Meteor.call('genURL', modelToShare, 'Original', false, Session.get('last_id'), themeData, handleGenURLEvent);
                }
            }
        }
    },
    'click #prev'(evt) {
        if (evt.toElement.id != 'prev') {
            if (!$('#prev > button').is(':disabled')) {
                const currentInstance = Session.get('currentInstance');
                if (currentInstance > 0) {
                    const instance = getCurrentInstance(currentInstance - 1);
                    if (instance) {
                        Session.set('currentInstance', currentInstance - 1);
                    } else {
                        const command = Session.get('commands').length > 1 ? $('.command-selection > select option:selected').text() : Session.get('commands')[0];

                        id = Router.current().params._id;
                        if (!id) {
                            id = 'Original';
                        }

                        Meteor.call('getInstance', textEditor.getValue(), Meteor.default_connection._lastSessionId, currentInstance - 1, command, true, id, null, handlePreviousInstanceEvent);
                    }
                    $('#next > button').prop('disabled', false);
                }
            }
        }
    },
    'click #next'(evt) {
        if (evt.toElement.id != 'next') {
            if (!$('#next > button').is(':disabled')) {
                const currentInstance = Session.get('currentInstance');

                const instance = getCurrentInstance(currentInstance + 1);

                if (instance) {
                    Session.set('currentInstance', currentInstance + 1);
                } else {
                    const command = Session.get('commands').length > 1 ? $('.command-selection > select option:selected').text() : Session.get('commands')[0];

                    id = Router.current().params._id;
                    if (!id) {
                        id = 'Original';
                    }
                    Meteor.call('getInstance', textEditor.getValue(), Meteor.default_connection._lastSessionId, currentInstance + 1, command, false, id, null, handleNextInstanceEvent);
                }
                $('#prev > button').prop('disabled', false);
            }
        }
    },
    'click #genInstanceUrl'() {
        const themeData = {
            atomSettings,
            relationSettings,
            generalSettings,
            currentFramePosition,
            currentlyProjectedTypes,
            metaPrimSigs,
            metaSubsetSigs,
        };

        // obter o id do Run correspondente à instancia atual no browser
        const runID = Session.get('instances')[0].runID;

        // Meteor.call('storeInstance', textEditor.getValue(), themeData, cy.json(), handleGenInstanceURLEvent);
        Meteor.call('storeInstance', runID, themeData, cy.json(), handleGenInstanceURLEvent);
    },
    'click #validateModel'() { // click on the validate button
        Meteor.call('validate', textEditor.getValue(), (err, res) => {
            if (err) console.error('Unable to connect to server');
            else {
                res = JSON.parse(res);
                if (!res.success) {
                    addErrorMarkerToGutter(res.errorMessage, res.errorLocation.line);
                } else { // success
                    swal({
                        title: 'The Model is Valid!',
                        text: "You're doing great!",
                        type: 'info',
                    });
                }
            }
        });
    },
});
/* Callbacks added with this method are called once when an instance of Template.alloyEditor is rendered into DOM nodes and put into the document for the first time. */
Template.alloyEditor.onRendered(() => {
    try {
        cy;
    } catch (e) {
        initGraphViewer('instance');
    }
    // Adds click effects to Buttons
    buttonsEffects();
    // Hide Next, Previous, Run... buttons on startup
    hideButtons();

    // If there's subscribed data, process it.
    let model;
    if (Router.current().data) model = Router.current().data();

    if (model && textEditor) {
        let themeData;
        if (model.instance) themeData = model.instance.theme;
        // Place model on text editor
        const result = model.whole;
        textEditor.setValue(result);
        // Load theme settings;
        if (themeData) {
            atomSettings = themeData.atomSettings;
            relationSettings = themeData.relationSettings;
            generalSettings = themeData.generalSettings;
            currentFramePosition = themeData.currentFramePosition;
            currentlyProjectedTypes = themeData.currentlyProjectedTypes;
            if (themeData.metaPrimSigs) metaPrimSigs = themeData.metaPrimSigs;
            if (themeData.metaSubsetSigs) metaSubsetSigs = themeData.metaSubsetSigs;
        }
        // Load graph JSON data in case of instance sharing.
        if (model.instance && cy) {
            $('#instanceViewer').show();
            // cy.add(Router.current().data().instance.elements);
            cy.add(model.instance.graph.elements);
            updateElementSelectionContent();
        }
    }

    // On tab/browser closure, terminate the user's session.
    $(window).bind('beforeunload', (e) => {
        // No longer necessary. Webservice automatically deletes session associated objects after a few hours idle.
    });
    try {
        cy;
    } catch (e) {
        initGraphViewer('instance');
    }
    // Right click menu styling
    $('.command-selection').hide();
    (function ($) {
        $(document).ready(() => {
            $('#cssmenu li.active').addClass('open').children('ul').show();
            $('#cssmenu li.has-sub>a').on('click', function () {
                $(this).removeAttr('href');
                const element = $(this).parent('li');
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
    }(jQuery));
    $('#optionsMenu').hide();
});

/* ------------- Server HANDLERS methods && Aux Functions ----------- */

/* nextbtn event handler after getInstance(textEditor.getValue) , currentInstance + 1 */
function handleNextInstanceEvent(err, result) {
    if (err) {
        swal(err.reason, '', 'error');
    } else if (result.unsat) {
        $('#next > button').prop('disabled', true);
        swal('No more satisfying instances!', '', 'error');
    } else {
        updateInstances(result);
        Session.set('currentInstance', result.number);
    }
}

/* Execbtn event handler
      result: getInstance(textEditor.getValue,..) */
function handleInterpretModelEvent(err, result) {
    $.unblockUI();
    $('#exec > button').prop('disabled', true);

    // Restart shareModel option
    const permalink = document.getElementById('permalink');
    if (permalink) permalink.remove();
    $('#genUrl > button').prop('disabled', false);

    // clear projection combo box
    let select = document.getElementsByClassName('framePickerTarget');

    if (select) select = select[0];
    if (select) {
        const length = select.options.length;
        for (i = 0; i < length; i++) {
            select.remove(0);
        }
    }

    $('#instanceViewer').hide();
    $('#log').empty();
    const command = $('.command-selection > select option:selected').text();

    if (err) {
        if (err.error == 502) {
            swal('Syntax Error!', '', 'error');
            const x = document.createElement('IMG');
            x.setAttribute('src', '/images/icons/error.png');
            x.setAttribute('width', '15');
            x.setAttribute('id', 'error');
            x.setAttribute('title', err.reason.msg);
            textEditor.setGutterMarker(err.reason.line - 1, 'error-gutter', x);
            textEditor.refresh();
            $('#next > button').prop('disabled', true);
            $('#prev > button').prop('disabled', true);
        }
    } else {
        if (result.commandType && result.commandType == 'check') {
            /* if the commandType == check */

            const log = document.createElement('div');
            log.className = 'col-lg-12 col-md-12 col-sm-12 col-xs-12';
            const paragraph = document.createElement('p');


            if (result.unsat) {
                $('#instancenav').hide();

                paragraph.innerHTML = `No counter-examples. ${command} solved!`;
                paragraph.className = 'log-complete';
            } else {
                paragraph.innerHTML = `Invalid solution, checking ${command} revealed a counter-example.`;
                paragraph.className = 'log-wrong';
                updateGraph(result);
            }

            log.appendChild(paragraph);
            /* div with id=log will appendChild(log) */
            $('#log')[0].appendChild(log);
        }

        /* if the commandType != check */
        if (result.unsat) {
            $('.empty-univ').fadeIn();
            $('#instanceViewer').hide();
            $('#genInstanceUrl').hide();
        } else {
            updateInstances(result);
            Session.set('currentInstance', 0);
        }

        if (result.last_id) {
            Session.set('last_id', result.last_id);
        }
    }
}

/* genUrlbtn event handler after genUrl method */
function handleGenURLEvent(err, result) {
    if (!err) {
        // if the URL was generated successfully, create and append a new element to the HTML containing it.
        const url = document.createElement('div');
        url.className = 'col-lg-12 col-md-12 col-sm-12 col-xs-12';
        var anchor = document.createElement('a');
        anchor.href = `/${result.public}`;
        anchor.className = 'urlinfo';
        anchor.innerHTML = `${window.location.origin}/${result.public}`;
        url.appendChild(anchor);


        let urlPrivate = '';
        if (result.private !== undefined) {
            urlPrivate = document.createElement('div');
            urlPrivate.className = 'col-lg-12 col-md-12 col-sm-12 col-xs-12';
            var anchor = document.createElement('a');
            anchor.href = `/${result.private}`;
            anchor.className = 'urlinfo';
            anchor.innerHTML = `${window.location.origin}/${result.private}`;
            urlPrivate.appendChild(anchor);
        }

        const clipboard = document.createElement('div');
        clipboard.className = 'col-lg-12 col-md-12 col-sm-12 col-xs-12';
        clipboard.innerHTML = "<button class='clipboardbutton cbutton cbutton--effect-boris'><img src='/images/icons/clipboard.png' /><i class='legend'>copy to clipboard</i></button></div>";

        const textcenter = document.createElement('div');
        textcenter.className = 'text-center';
        textcenter.id = 'permalink';

        if (result.private !== undefined) {
            let paragraph = document.createElement('p');

            let text = document.createTextNode('Public Link:  ');
            paragraph.appendChild(text);
            paragraph.appendChild(url);

            textcenter.appendChild(paragraph);
            paragraph = document.createElement('p');

            text = document.createTextNode('Private Link:  ');
            paragraph.appendChild(text);
            paragraph.appendChild(urlPrivate);
            textcenter.appendChild(paragraph);
        } else {
            textcenter.appendChild(url);
        }

        textcenter.appendChild(clipboard);

        document.getElementById('url-permalink').appendChild(textcenter);
        $('#genUrl > button').prop('disabled', true);
        zeroclipboard();

        if (result.last_id) {
            Session.set('last_id', result.last_id);
        }
    }
}

/* prevbtn event handler after getInstance(textEditor.getValue) , currentInstance - 1 */
function handlePreviousInstanceEvent(err, result) {
    if (err) {
        swal(err.reason, '', 'error');
    } else {
        if (result.unsat) $('#prev > button').prop('disabled', true);
        else updateInstances(result);
        Session.set('currentInstance', result.number);
    }
}

/* geninstanceurlbtn event handler after storeInstance method */
function handleGenInstanceURLEvent(err, result) {
    if (!err) {
        // if the URL was generated successfully, create and append a new element to the HTML containing it.
        const url = document.createElement('div');
        url.className = 'col-lg-12 col-md-12 col-sm-12 col-xs-12';
        const anchor = document.createElement('a');
        anchor.href = `/${result}`;
        anchor.target = '_';
        anchor.className = 'urlinfo';
        anchor.innerHTML = `${window.location.origin}/${result}`;
        url.appendChild(anchor);

        const clipboard = document.createElement('div');
        clipboard.className = 'col-lg-12 col-md-12 col-sm-12 col-xs-12';
        clipboard.innerHTML = "<button class='clipboardbutton cbutton cbutton--effect-boris'><img src='/images/icons/clipboard.png' /><i class='legend'>copy to clipboard</i></button></div>";


        const textcenter = document.createElement('div');
        textcenter.className = 'text-center';
        textcenter.id = 'instance_permalink';
        textcenter.appendChild(url);
        textcenter.appendChild(clipboard);

        document.getElementById('url-instance-permalink').appendChild(textcenter);
        $('#genInstanceUrl > button').prop('disabled', true);
        zeroclipboard();
    }
}

/* Functions used to update session instances */
updateInstances = function (instance) {
    if (!Session.get('instances')) {
        var instances = [instance];
        Session.set('instances', instances);
        Session.set('currentInstance', 0);
    } else {
        var instances = Session.get('instances');
        instances.push(instance);
        Session.set('instances', instances);
        Session.set('currentInstance', Session.get('currentInstance'));
    }
};
getCurrentInstance = function (instanceNumber) {
    const instances = Session.get('instances');
    let result;
    instances.forEach((inst) => {
        if (inst.number == instanceNumber) {
            result = inst;
        }
    });
    return result;
};

/* onRendered aux functions */
function buttonsEffects() {
    function mobilecheck() {
        let check = false;
        (function (a) {
            if (/(android|ipad|playbook|silk|bb\d+|meego).+mobile|avantgo|bada\/|blackberry|blazer|compal|elaine|fennec|hiptop|iemobile|ip(hone|od)|iris|kindle|lge |maemo|midp|mmp|netfront|opera m(ob|in)i|palm( os)?|phone|p(ixi|re)\/|plucker|pocket|psp|series(4|6)0|symbian|treo|up\.(browser|link)|vodafone|wap|windows (ce|phone)|xda|xiino/i.test(a) || /1207|6310|6590|3gso|4thp|50[1-6]i|770s|802s|a wa|abac|ac(er|oo|s\-)|ai(ko|rn)|al(av|ca|co)|amoi|an(ex|ny|yw)|aptu|ar(ch|go)|as(te|us)|attw|au(di|\-m|r |s )|avan|be(ck|ll|nq)|bi(lb|rd)|bl(ac|az)|br(e|v)w|bumb|bw\-(n|u)|c55\/|capi|ccwa|cdm\-|cell|chtm|cldc|cmd\-|co(mp|nd)|craw|da(it|ll|ng)|dbte|dc\-s|devi|dica|dmob|do(c|p)o|ds(12|\-d)|el(49|ai)|em(l2|ul)|er(ic|k0)|esl8|ez([4-7]0|os|wa|ze)|fetc|fly(\-|_)|g1 u|g560|gene|gf\-5|g\-mo|go(\.w|od)|gr(ad|un)|haie|hcit|hd\-(m|p|t)|hei\-|hi(pt|ta)|hp( i|ip)|hs\-c|ht(c(\-| |_|a|g|p|s|t)|tp)|hu(aw|tc)|i\-(20|go|ma)|i230|iac( |\-|\/)|ibro|idea|ig01|ikom|im1k|inno|ipaq|iris|ja(t|v)a|jbro|jemu|jigs|kddi|keji|kgt( |\/)|klon|kpt |kwc\-|kyo(c|k)|le(no|xi)|lg( g|\/(k|l|u)|50|54|\-[a-w])|libw|lynx|m1\-w|m3ga|m50\/|ma(te|ui|xo)|mc(01|21|ca)|m\-cr|me(rc|ri)|mi(o8|oa|ts)|mmef|mo(01|02|bi|de|do|t(\-| |o|v)|zz)|mt(50|p1|v )|mwbp|mywa|n10[0-2]|n20[2-3]|n30(0|2)|n50(0|2|5)|n7(0(0|1)|10)|ne((c|m)\-|on|tf|wf|wg|wt)|nok(6|i)|nzph|o2im|op(ti|wv)|oran|owg1|p800|pan(a|d|t)|pdxg|pg(13|\-([1-8]|c))|phil|pire|pl(ay|uc)|pn\-2|po(ck|rt|se)|prox|psio|pt\-g|qa\-a|qc(07|12|21|32|60|\-[2-7]|i\-)|qtek|r380|r600|raks|rim9|ro(ve|zo)|s55\/|sa(ge|ma|mm|ms|ny|va)|sc(01|h\-|oo|p\-)|sdk\/|se(c(\-|0|1)|47|mc|nd|ri)|sgh\-|shar|sie(\-|m)|sk\-0|sl(45|id)|sm(al|ar|b3|it|t5)|so(ft|ny)|sp(01|h\-|v\-|v )|sy(01|mb)|t2(18|50)|t6(00|10|18)|ta(gt|lk)|tcl\-|tdg\-|tel(i|m)|tim\-|t\-mo|to(pl|sh)|ts(70|m\-|m3|m5)|tx\-9|up(\.b|g1|si)|utst|v400|v750|veri|vi(rg|te)|vk(40|5[0-3]|\-v)|vm40|voda|vulc|vx(52|53|60|61|70|80|81|83|85|98)|w3c(\-| )|webc|whit|wi(g |nc|nw)|wmlb|wonu|x700|yas\-|your|zeto|zte\-/i.test(a.substr(0, 4))) check = true;
        }(navigator.userAgent || navigator.vendor || window.opera));
        return check;
    }

    const support = {
        animations: Modernizr.cssanimations,
    };


    const animEndEventNames = {
        WebkitAnimation: 'webkitAnimationEnd',
        OAnimation: 'oAnimationEnd',
        msAnimation: 'MSAnimationEnd',
        animation: 'animationend',
    };


    const animEndEventName = animEndEventNames[Modernizr.prefixed('animation')];


    const onEndAnimation = function (el, callback) {
        var onEndCallbackFn = function (ev) {
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
    };


    const eventtype = mobilecheck() ? 'touchstart' : 'click';

    [].slice.call(document.querySelectorAll('.cbutton')).forEach((el) => {
        el.addEventListener(eventtype, () => {
            classie.add(el, 'cbutton--click');
            onEndAnimation(classie.has(el, 'cbutton--complex') ? el.querySelector('.cbutton__helper') : el, () => {
                classie.remove(el, 'cbutton--click');
            });
        });
    });
}

function hideButtons() {
    $('#exec > button').prop('disabled', true);
    $('#next > button').prop('disabled', true);
    $('#prev > button').prop('disabled', true);
    $('.permalink > button').prop('disabled', true);
}

function zeroclipboard() {
    const client = new ZeroClipboard($('.clipboardbutton'));
    client.on('copy', (event) => {
        const clipboard = event.clipboardData;
        clipboard.setData('text/plain', $('.urlinfo').html());
    });
}


/*
  Check if the model contains some valid 'secret'
*/
function containsValidSecret(model) {
    let i; let j; let
        lastSecret = 0;
    let paragraph = '';
    while ((i = model.indexOf('//SECRET\n', lastSecret)) >= 0) {
        for (var z = i + ('//SECRET\n'.length);
            (z < model.length && model[z] != '{'); z++) {
            paragraph += model[z];
        }
        if (!isParagraph(paragraph)) {
            paragraph = '';
            lastSecret = i + 1;
            continue;
        }
        if (findClosingBracketMatchIndex(model, z) != -1) {
            return true;
        }
        lastSecret = i + 1;
    }
    return false;
}

function containsValidSecretWithAnonymousCommand(model) {
    let lastSecret = 0;
    while ((i = model.indexOf('//SECRET\n', lastSecret)) >= 0) {
        const s = model.substr(i + '//SECRET\n'.length).trim();
        // se o resto do texto comecar com a expressao abaixo entao contem
        // um comando anonimo
        if (s.match('^(assert|run|check)([ \t\n])*[{]')) {
            return true;
        }

        lastSecret = i + 1;
    }
    return false;
}

function findClosingBracketMatchIndex(str, pos) {
    if (str[pos] != '{') {
        throw new Error(`No '{' at index ${pos}`);
    }
    let depth = 1;
    for (let i = pos + 1; i < str.length; i++) {
        switch (str[i]) {
        case '{':
            depth++;
            break;
        case '}':
            if (--depth == 0) {
                return i;
            }
            break;
        }
    }
    return -1; // No matching closing parenthesis
}
