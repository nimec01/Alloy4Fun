import {
    Model
} from '../../lib/collections/model'
import {
    Link
} from '../../lib/collections/link'
import {
    containsValidSecret
} from "../../lib/editor/text"

/**
 * Meteor method to get a model share URL
 * Stores the model specified in the function argument
 * @return The 'id' of the model link, used in Share Model option
 */
Meteor.methods({
    genURL: function(code, last_id) {
        // A Model is always created, regardless of having secrets or not
        let model = {
            code: code,
            time: new Date().toLocaleString()
        }
        // explicitly set optional to avoid nulls
        if (last_id) model.derivationOf = last_id
        // insert
        let model_id = Model.insert(model);

        //Generate the public link
        let public_link_id = Link.insert({
            model_id: model_id,
            private: false
        });

        //Generate private link if SECRET is present
        let private_link_id
        if (containsValidSecret(code)) {
            private_link_id = Link.insert({
                model_id: model_id,
                private: true
            });
        }

        return {
            public: public_link_id,
            private: private_link_id, // will be undefined if no secret is present
            last_id: model_id
        }
    }
});


// Helper functions


// function findClosingBracketMatchIndex(str, pos) {
//     if (str[pos] != '{') {
//         throw new Error("No '{' at index " + pos);
//     }
//     var depth = 1;
//     for (var i = pos + 1; i < str.length; i++) {
//         switch (str[i]) {
//             case '{':
//                 depth++;
//                 break;
//             case '}':
//                 if (--depth == 0) {
//                     return i;
//                 }
//                 break;
//         }
//     }
//     return -1; // No matching closing parenthesis
// }