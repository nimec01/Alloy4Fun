import {
    Meteor
} from "meteor/meteor";

/**
 * Receives a link (private or public) and returns the corresponding Model
 * with or without the SECRET code
 */
Meteor.methods({
    getModel: function(linkId) {
		let link = Link.findOne(linkId)
		console.log("getmodel link:", link);
		let model = Model.findOne(link.model_id)
		console.log(model);
		if(link.isPrivate){
			console.log("private link");
		}
		return model
	},
	getInstance: function(linkId){
		
	}
})