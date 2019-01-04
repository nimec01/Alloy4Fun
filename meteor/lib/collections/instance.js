/**
 * An instance was generated from a command with ref to the model
 */

Instance = new Meteor.Collection('Instance');

//TODO: colocar aqui esquema correto, model_id não está a ser utilizado


Instance.attachSchema(new SimpleSchema({
    _id: {
        type: String,
        optional: false
    },
    model_id: {
        type: String,
        optional: false
    },
    sat: { // was the command satisfied?
        type: Boolean,
        optional: false
    },
    command: { // name of the command that was executed to generate instance
        type: String,
        optional: false
    },
    graph: { // the entire cytoscape graph
        type: Object,
        optional: false,
        blackbox: true
    },
    theme: { // the theme associated with this instance
        type: Object,
        optional: false,
        blackbox: true
    },
    time: {
        type: String
    }
}))

export {
    Instance
};