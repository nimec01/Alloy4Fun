import {
    extractSecrets,
    containsValidSecret
} from "../../lib/editor/text"

Meteor.methods({
    /**
      * Meteor method to execute the current model and get model instances.
      * This will call the Alloy API (webService). If the model contains
      * secrets and the previous didn't (if any), will become a new derivation
      * root (although it still registers the derivation).
      * 
      * @param {String} code the Alloy model to execute
      * @param {Number} commandIndex the index of the command to execute
      * @param {Boolean} commandType whether the command was a run (true) or
      *     check (false)
      * @param {String} currentModelId the id of the current model (from which
      *     the new will derive)
      * 
      * @returns the instance data and the id of the new saved model
      */
    getInstances: function(code, commandIndex, commandType, currentModelId) {
        return new Promise((resolve, reject) => {
            // if no secrets, try to extract from original
            let code_with_secrets = code
            if (currentModelId && !containsValidSecret(code)) {
                let o = Model.findOne(currentModelId).original
                code_with_secrets = code + extractSecrets(Model.findOne(o).code).secret                    
            }

            // save executed model to database
            let new_model = {
                // original code, without secrets
                code: code,
                command: commandIndex,
                time: new Date().toLocaleString(),
                derivationOf: currentModelId,
            }

            // insert the new model
            let new_model_id = Model.insert(new_model);

            // call webservice to get instances
            HTTP.call('POST', `${Meteor.settings.env.API_URL}/getInstances`, {
                data: {
                    model: code_with_secrets,
                    numberOfInstances: Meteor.settings.env.MAX_INSTANCES,
                    commandIndex: commandIndex,
                    sessionId: new_model_id
                }
            }, (error, result) => {
                if (error) reject(error)

                let content = JSON.parse(result.content);
                // if unsat, still list with single element
                let sat
                Object.keys(content).forEach(k => {
                    content[k].commandType = commandType;
                    sat = content[k].unsat;
                });

                let original
                // if the model has secrets and the previous hadn't, then it is a new root
                if (!currentModelId || (containsValidSecret(code) && !containsValidSecret(Model.findOne(currentModelId).code))) {
                    original = new_model_id
                } 
                // otherwise inherit the root
                else {
                    original = Model.findOne(currentModelId).original
                }

                // update the root
                Model.update({ _id : new_model_id },{$set: {original : original, sat : sat}})

                // resolve the promise
                resolve({
                    instances: content,
                    newModelId: new_model_id
                });
            });
        })
    }
});