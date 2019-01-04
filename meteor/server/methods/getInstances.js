/**
 * Meteor method to get a model instance
 * This will call the API (webService)
 * @param model the Alloy code to validate
 * @param sessionId the id of the session
 * @param instanceNumber the index of the instance to retrieve
 * @param commandLabel (alloy commands [run, check, assert, ...])
 * @param forceInterpretation used to skip cache and force new model interpretation
 * @param cid link_id of the 'derivatedOf' model (original otherwise)
 * @param last_id
 * @returns Object with the instance data
 */
Meteor.methods({
    //TODO: Daniel, é mesmo suposto manter o forceInterpretation?
    getInstances: function(model, numberOfInstances, commandLabel, forceInterpretation) {
        return new Promise((resolve, reject) => {
            HTTP.call('POST', `${Meteor.settings.env.API_URL}/getInstances`, {
                data: {
                    model: model,
                    numberOfInstances: numberOfInstances,
                    commandLabel: commandLabel,
                    forceInterpretation: forceInterpretation
                }
            }, (error, result) => {
                if (error) reject(error)
                let content = JSON.parse(result.content)
                if (content.unsat == true) {
                    content.commandType = "check";
                    resolve(content);
                } else {
                    Object.keys(content).forEach(k => {
                        content[k].commandType = "check";
                    });
                    resolve(content);
                }
            });
        })
    }
});