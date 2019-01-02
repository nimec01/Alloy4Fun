/**
 * Link merely links to Models. 
 * When a challenge is created two links are provided: one public and another private.
 * This corresponds to two Link instances (with different _ids) 
 * but both have the same model_id as they both point to same model. 
 */
_Link = new Meteor.Collection('Link');

_Link.attachSchema(new SimpleSchema({
    _id: {
        type: String,
        optional: false
    },
    private: { // wether this is a private or public link (shows SECRETs for private)
        type: Boolean,
        optional: false
    },
    model_id: { // the id of the model associated
        type: String,
        optional: false
    }
}))

export let Link = _Link;