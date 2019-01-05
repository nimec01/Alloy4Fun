import {
    extractSecrets
} from "../lib/secrets"
/**
 * Meteor method to get a model instance
 * This will call the API (webService)
 * @param code the Alloy code to validate
 * @param instanceNumber the index of the instance to retrieve
 * @param commandLabel (alloy commands [run, check, assert, ...])
 * @param forceInterpretation used to skip cache and force new model 
 * @param last_id the model this one derives from
 * @param from_private false means it was loaded from public link and must retrieve //SECRET code
 * @returns Object with the instance data
 */
Meteor.methods({
    //TODO: Daniel, é mesmo suposto manter o forceInterpretation?
    getInstances: function(code, numberOfInstances, commandLabel, forceInterpretation, last_id, original, from_private) {
        return new Promise((resolve, reject) => {
            if (from_private) { //if public link was used, load secrets
                //load original model, extract secrets and append to code
                code += extractSecrets(Model.findOne(original).whole).secret
            }
            console.log(from_private, code);
            
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

                // handle result (unsat vs sat)
                let content = JSON.parse(result.content)
                if (content.unsat) { // no counter-examples found
                    content.commandType = "check";
                } else { // counter-examples found
                    Object.keys(content).forEach(k => {
                        content[k].commandType = "check";
                    });
                }

                // save executed model to database
                let new_model = {
                    whole: code,
                    command: commandLabel,
                    sat: !!content.unsat, // sat means there was no counter-example (!! is for bool)
                    time: new Date().toLocaleString()
                }
                // optional params explictly to avoid_idnull
                if (last_id) new_model.derivationOf = last_id
                if (original) new_model.original = original
                // insert
                let model_id = Model.insert(new_model);

                // resolve the promise
                resolve({
                    instances: content,
                    last_id: model_id
                });
            });
        })
    }
});