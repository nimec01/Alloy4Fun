/**
 * Publishes the link information to the client
 */
Meteor.publish("link", function(linkId) {
    return Link.find({
        _id: linkId
    }, {
        fields: Link.publicFields
    })
});