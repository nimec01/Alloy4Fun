import classie from 'classie';
import 'qtip2/src/core.css';

import {
    getCommandsFromCode
} from "../../../lib/editor/text"
import {
    generateModelURL,
    generateInstURL
} from "./genUrl"
import {
    executeModel,
    nextModel,
    prevModel
} from "./executeModel"
import {
    downloadTree
} from "./downloadTree"
import {
    copyToClipboard
} from "../../lib/editor/clipboard"
import {
    cmdChanged,
    isUnsatInstance
} from "../../lib/editor/state"

/* Each template has a local dictionary of helpers that are made available to
it, and this call specifies helpers to add to the template’s dictionary.*/
Template.alloyEditor.helpers({

    /** 
     * Whether the execute command button is enabled, if the model has not
     * been updated and the selected command has not been changed. 
     */
    execEnabled() {
        const commands = Session.get('commands');
        const enab = Session.get('model-updated') && commands.length > 0;
        return enab ? "" : "disabled";
    },

    /** 
     * Whether the next instance button should be enabled, if the current
     * instance is not the last and the model has not been updated.
     */
    nextInstEnabled() {
        const instanceIndex = Session.get('currentInstance');
        const maxInstanceNumber = Session.get('maxInstance');
        const enab = !Session.get('model-updated') && instanceIndex!=maxInstanceNumber;
        return enab ? "" : "disabled";
    },

    /** 
     * Whether the previous instance button should be enabled, if the current
     * instance is not the first and the model has not been updated.
     */
    prevInstEnabled() {
        const instanceIndex = Session.get('currentInstance');
        const enab = !Session.get('model-updated') && instanceIndex!=0;
        return enab ? "" : "disabled";
    },

    /** 
     * Whether to enable the sharing of models, when the model has not been
     * already shared and the model is not empty.
     */
    shareModelEnabled() {
        const enab = !Session.get('model-shared') && !Session.get('empty-model');
        return enab ? "" : "disabled";
    },

    /**
     * Whether to show model links, when the model has been shared and the
     * model is not empty.
     */
    showModelLinks() {
        const enab = Session.get('model-shared') && !Session.get('empty-model');
        return enab;
    },

    /** 
     * Whether to enable the downloading of the derivation tree, if currently
     * on a shared private link.
     */
    downloadTreeEnabled() {
        const enab = Session.get('from_private');
        return enab ? "" : "disabled";        
    },

    /** 
     * Whether to enable the sharing of instances, when the instance has not
     * been already shared and is not showing a static shared instance.
     */
    shareInstEnabled() {
        const enab = !Session.get('inst-shared') && !Session.get('from-instance');
        return enab ? "" : "disabled";
    },

    /**
     * Whether to show instance links, when the instance has been shared.
     */
    showInstanceLinks() {
        const enab = Session.get('inst-shared');
        return enab;
    },

    /** 
     * Whether instance elements should be shown. They will be shown when
     * there are instances stored, unless there is a single instance that is
     * unsat, or when coming from a shared instance. If maxInstance == 0 also
     * shows, used to initialize the process.
     */
    showInstance() {
        const m = Session.get('maxInstance');
        const i = Session.get('currentInstance');
        const s = Session.get('from-instance');
        return (s || m==0 || (m>0 && (m!=1 || !isUnsatInstance(0))))?"":"hidden";
    },

    /** 
     * The list of commands, including those hidden inherited. 
     */
    getCommands() {
        const commands = Session.get('commands');
        return commands?commands:[];
    },

    /** 
     * Whether to show the command combobox, if there is more than one defined.
     */
    showCommands() {
        const commands = Session.get('commands');
        return commands?commands.length > 1:false;
    },

    /**
     * Whether the model inherits secrets from the root of the derivation,
     * and has not override them with local secrets.
     */
    inheritsSecrets() {
        const cmds = Session.get('hidden_commands');
        const inherits = cmds?cmds.length > 0:false;
        const hasLocal = Session.get('local-secrets');
        return inherits && !hasLocal;
    },

    /**
     * Whether the model has local secrets defined.
     */
    hasLocalSecrets() {
        return Session.get('local-secrets');
    },

    /**
     * The logging message to be presented.
     */
    logMessage() {
        return Session.get('log-message');
    },

    /**
     * The logging class to be presented.
     */
    logClass() {
        return Session.get('log-class');
    },

    /**
     * The current private model sharing URL.
     */
    privateModelURL() {
        return Session.get('private-model-url');
    },

    /**
     * The current public model sharing URL.
     */
    publicModelURL() {
        return Session.get('public-model-url');
    },

    /**
     * The current instance sharing URL.
     */
    instanceURL() {
        return Session.get('inst-url');
    },

});

Template.alloyEditor.events({
    'keydown': function(e) { 
        if (e.ctrlKey && e.key == 'e')
            $('#exec > button').trigger("click");
    },
    'click #exec > button': executeModel,
    'change .command-selection > select'() {
        cmdChanged();
    },
    'click #genUrl > button': generateModelURL,
    'click #prev > button': prevModel,
    'click #next > button': nextModel,
    'click #genInstanceUrl > button': generateInstURL,
    'click #downloadTree > button': downloadTree,
    'click .clipboardbutton': function (evt) {
        copyToClipboard(evt)
    },
});

/* 
 * Callbacks added with this method are called once when an instance of
 * Template.alloyEditor is rendered into DOM nodes and put into the document
 * for the first time.
 */
Template.alloyEditor.onRendered(() => {
    Session.set('empty-model',true);
    Session.set('model-updated',false);
    Session.set('inst-shared',false)
    Session.set('currentInstance',0);
    Session.set('maxInstance',-1);
    Session.set('commands',[]);
    Session.set('local-secrets',false);
    Session.set('model-shared',false);
    Session.set('from-instance',false);

    // if there's subscribed data, process it
    if (Router.current().data && textEditor) { 
        // load the model from controller
        let model = Router.current().data(); 
        // save the loaded model id for later derivations
        Session.set('last_id', model.model_id); 
        // whether the followed link was private
        Session.set('from_private', model.from_private);
        // retrieved the inherited secret commands
        Session.set('hidden_commands', model.sec_commands); 
        let cs = getCommandsFromCode(model.code)
        if (model.sec_commands) cs.concat(model.sec_commands)
        // register all available commands
        Session.set('commands', cs) 

        // update the textEditor
        textEditor.setValue(model.code); 

        // retrieve the shared theme
        let themeData = model.theme;
        if (themeData) {    
            atomSettings = themeData.atomSettings;
            relationSettings = themeData.relationSettings;
            generalSettings = themeData.generalSettings;
            currentFramePosition = themeData.currentFramePosition;
            currentlyProjectedTypes = themeData.currentlyProjectedTypes;
            if (themeData.metaPrimSigs) metaPrimSigs = themeData.metaPrimSigs;
            if (themeData.metaSubsetSigs) metaSubsetSigs = themeData.metaSubsetSigs;
            if (currentlyProjectedTypes.length != 0) staticProjection();
        }

        // if a shared instance, process it
        if (model.instance) { 
            Session.set('from-instance',true);
            Session.set('log-message','Static shared instance. Execute model to iterate.')
            Session.set('log-class','log-info')
            initGraphViewer('instance');
            // load graph JSON data 
            if (cy) {
                cy.add(model.instance.graph.elements);
                updateElementSelectionContent();
                cy.zoom(model.instance.graph.zoom);
                cy.pan(model.instance.graph.pan);
            }
        }
    } 
    // else, a new model
    else {
        Session.set('from_private', undefined);
    }
    // add click effects to buttons
    buttonsEffects();
    // and right click menu
    styleRightClickMenu();
});

// button click effects
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

// right click menu styling
function styleRightClickMenu() {
    (function ($) {
        $(document).ready(function () {
            $('#cssmenu li.active').addClass('open').children('ul').show();
            $('#cssmenu li.has-sub>a').on('click', function () {
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
}