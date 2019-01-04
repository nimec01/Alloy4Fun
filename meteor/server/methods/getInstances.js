/**
 * Meteor method to get a model instance
 * This will call the API (webService)
 * @param code the Alloy code to validate
 * @param instanceNumber the index of the instance to retrieve
 * @param commandLabel (alloy commands [run, check, assert, ...])
 * @param forceInterpretation used to skip cache and force new model 
 * @param last_id the model this one derives from
 * @returns Object with the instance data
 */
Meteor.methods({
    //TODO: Daniel, é mesmo suposto manter o forceInterpretation?
    getInstances: function(code, numberOfInstances, commandLabel, forceInterpretation, last_id) {
        return new Promise((resolve, reject) => {
            // save executed model to database
            let model_id = Model.insert({
                whole: code,
                derivationOf: last_id || "Original",
                command: commandLabel,
                time: new Date().toLocaleString()
            });

            // call webservice to get instances
            HTTP.call('POST', `${Meteor.settings.env.API_URL}/getInstances`, {
                data: {
                    model: code,
                    numberOfInstances: numberOfInstances,
                    commandLabel: commandLabel,
                    forceInterpretation: forceInterpretation
                }
            }, (error, result) => {
                if (error) reject(error)
                let content = JSON.parse(result.content)
                if (content.unsat) { // unsatisfied single check
                    content.commandType = "check";
                } else {
                    Object.keys(content).forEach(k => {
                        content[k].commandType = "check";
                    });
                }
                resolve({
                    instances: content,
                    last_id: model_id
                });
            });
        })
    }
});