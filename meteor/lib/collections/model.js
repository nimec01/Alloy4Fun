/**
 * Models created through the editor feature.
 * An Alloy model
 */

Model = new Meteor.Collection('Model');

Model.attachSchema(new SimpleSchema({
    _id: {
        type: String,
        optional: false
    },
    whole: { // has all of the code
        type: String,
        optional: false
    },
    derivationOf: { // which model does it derive from
        type: String
    },
    time: {
        type: String
    }
}));