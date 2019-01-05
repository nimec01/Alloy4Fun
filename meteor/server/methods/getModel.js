import {
    Meteor
} from "meteor/meteor";
import {
    extractSecrets
} from "../lib/secrets"
import {getCommandsFromCode} from "../../lib/editor/text";
/**
 * Receives a link (private or public) and returns the corresponding Model
 * with or without the SECRET code
 */
Meteor.methods({
    getModel: function(linkId) {
        let link = Link.findOne(linkId)
        let model = Model.findOne(link.model_id)
        if (!link.private) model.whole = extractSecrets(model.whole).public
        model.model_id = model._id // this is necessary because publish is for the linkId
        model.from_private = link.private // return info about the used link type
        model.commands = getCommandsFromCode(model.whole)
        return model
    },
    getInstance: function(linkId) {
        //TODO: 
    }
})