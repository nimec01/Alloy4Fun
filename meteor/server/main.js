import { Meteor } from 'meteor/meteor';

import '../server/methods/validate'
import '../server/methods/genURL'
import '../server/methods/getInstances'
import '../server/methods/getProjection'
import '../server/methods/shareInstance'
import '../server/methods/getModel'

import './publications/modelFromLink'

Meteor.startup(() => {
    // code to run on server at startup
});
