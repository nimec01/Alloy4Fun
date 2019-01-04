/**
 * Models created through the editor feature.
 * An Alloy model, with the code
 */

Model = new Meteor.Collection('Model');

Model.attachSchema(new SimpleSchema({
    _id: {
        type: String
    },
    whole: { // has all of the code
        type: String
    },
    derivationOf: { // which model does it derive from
        type: String
    },
    /**
     * optional field for the command selected when model was created.
     * genUrl will not set command
     * execute will set the command
     */
    command: {
        type: String,
        optional: true
    },
    time: {
        type: String
    }
}))

Model.publicFields = {
    whole: 1,
    derivationOf: 1
}
export {
    Model
};