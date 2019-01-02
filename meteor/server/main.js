import {
    Meteor
} from 'meteor/meteor';

import '../server/methods/validate'
import '../server/methods/genURL'
import '../server/methods/getInstance'
import '../server/methods/getProjection'
import '../server/methods/shareInstance'

Meteor.startup(() => {
    // code to run on server at startup

    Todos = new Mongo.Collection('todos');
    console.log("MONGO IS ALIVE");
});