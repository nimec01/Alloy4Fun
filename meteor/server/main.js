import {
    Meteor
} from 'meteor/meteor';
import {
    seedWithModels
} from "./seed"

import '../server/methods/validate'
import '../server/methods/genURL'
import '../server/methods/getInstances'
import '../server/methods/getProjection'
import '../server/methods/shareInstance'
import '../server/methods/getModel'
import '../server/methods/downloadTree'

import './publications/modelFromLink'

/**
  If the database is empty, seeds a set of default models after startup.
*/
Meteor.startup(() => {
    if (!Model.find().count()) {
        // if there are no models, insert default ones
        let res = seedWithModels()
        console.log(`Seeded Database with ${res} models`);
    }
});