/**
 * Result of executing a Model.
 * Saves the model, the command itself and the result
 */

Run = new Meteor.Collection('Run');

Run.attachSchema(new SimpleSchema({
    _id: {
        type: String,
        optional: false
    },
    sat: { // was the command satisfied?
        type: Boolean,
        optional: false
    },
    model: { // TODO: Atenção aqui fica o model_id != "model" : Alterar
        type: String,
        optional: false
    },
    command: { // name of the command that was executed
        type: String,
        optional: false
    },
    time: {
        type: String
    }
}))

export {
    Run
};