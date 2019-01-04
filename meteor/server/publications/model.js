/**
 * Publishes the model information to the client
 * this takes care of hiding secrets where they should be hidden
 */
Meteor.publish("model", function(modelId) {
    return Model.find({
        _id: modelId
    }, {
        fields: Model.publicFields
    })
});