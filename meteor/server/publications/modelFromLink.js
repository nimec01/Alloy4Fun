/**
 * Publishes the link information to the client
 */
Meteor.publish("modelFromLink", function(linkId) {
    let publication = this;
    Meteor.call("getModel", linkId, (err, model) => {
        if (!err) publication.added('Model', linkId, model);
        // see https://docs.meteor.com/api/pubsub.html#Subscription-added
        publication.ready();
    })
})