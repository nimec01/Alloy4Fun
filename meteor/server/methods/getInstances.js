import {
    extractSecrets
} from "../lib/secrets"

/**
  Meteor method to execute the current model and get model instances. This
  will call the Alloy API (webService). Will set the current model as the one
  will be derived, and inherit the original root node from it.

  @param {String} code the Alloy model to execute
  @param {String} commandLabel the label of the command to execute
  @param {String} currentModelId the id of the current model (from which the
      new will derive)
  @param {Boolean} from_private whether it was loaded from public link and
      must retrieve secrets

  @returns the instance data and the id of the new saved model
 */
Meteor.methods({
    getInstances: function(code, commandLabel, currentModelId, from_private) {
        return new Promise((resolve, reject) => {
            let originalId = undefined
            let code_with_secrets = code
            if (currentModelId) {
                // retrieve root derivation node
                originalId = Model.findOne(currentModelId).original
                // if no root, set parent as root
                if (!originalId) originalId = currentModelId
                if (from_private === false) { 
                    // if public link was used, load secrets from original model
                    code_with_secrets = code + extractSecrets(originalId).secret
                }
            } else {
                currentModelId = undefined
            }

            // call webservice to get instances
            HTTP.call('POST', `${Meteor.settings.env.API_URL}/getInstances`, {
                data: {
                    model: code_with_secrets,
                    numberOfInstances: Meteor.settings.env.MAX_INSTANCES,
                    commandLabel: commandLabel
                }
            }, (error, result) => {
                if (error) reject(error)

                // handle result (unsat vs sat)
                let content = JSON.parse(result.content);
                if (content.unsat) { // no counter-examples found
                    content.commandType = "check";
                } else { // counter-examples found
                    Object.keys(content).forEach(k => {
                        content[k].commandType = "check";
                    });
                }

                // save executed model to database
                let new_model = {
                    // original code, without secrets
                    code: code,
                    command: commandLabel,
                    // sat means there was no counter-example (!! is for bool)
                    sat: !!content.unsat, 
                    time: new Date().toLocaleString(),
                    // will be undefined if no current model
                    derivationOf: currentModelId,
                    // will be undefined if no current model
                    original: originalId
                }
                // insert the new model
                let new_model_id = Model.insert(new_model);

                // assign the original root to itself if no previous model
             //   if (!originalId) {
             //       Model.update({ "_id": new_model_id }, {$set: { "original": new_model_id }})
             //   }

                console.log("** new model after exec")
                console.log(new_model)

                // resolve the promise
                resolve({
                    instances: content,
                    newModelId: new_model_id
                });
            });
        })
    }
});