/**
 * Meteor method to store a model instance with the user-defined theme
 * Used in Share Instance option
 * Creates an instance that points to the model
 * Saves the theme in the instance itself
 */
Meteor.methods({
    /**
     * Saves the instance and returns the id
     * @param {String} modelId the model id
     * @param {Bool} sat true or fals for satisfiable instance or not
     * @param {String} command the name of the command that was executed
     * @param {String} instance the JSON string of the cytoscape graph
     * @param {Object} themeData with the theme information for cytoscape
     * @return id of the new instance
     */
    storeInstance: function(modelId, sat, command, instance, themeData) {
        return Instance.insert({
            model_id: modelId,
            sat: sat,
            command: command,
            graph: instance,
            theme: themeData,
            date: new Date().toLocaleString()
        });
    }
});