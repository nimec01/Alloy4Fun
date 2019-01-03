/**
 * An instance was generated from a command with ref to the model
 */

Instance = new Meteor.Collection('Instance');

// TODO: colocar aqui esquema correto, model_id não está a ser utilizado

/*
Instance.attachSchema(new SimpleSchema({
    _id: {
        type: String,
        optional: false
    },
    model: {
        type: String,
        optional: false
    },
    run_id: {
        type: String,
        optional: false
    },
    graph: { // the whole graph
        type: Object,
        optional: false,
        blackbox: true
    },
    theme: { // o tema associado à instância
        type: Object,
        optional: false,
        blackbox: true
    }
})) */

export { Instance };
