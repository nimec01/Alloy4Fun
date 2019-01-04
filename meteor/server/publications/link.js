/**
 * Publishes the link information to the client
 */
Meteor.publish("link", function(linkId) {
    let publication = this;
    Meteor.call("getModel", linkId, (err, model) => {
        if(err) publication.ready();
        publication.added('Model', linkId, model);
        publication.ready();
    })
})