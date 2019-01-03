import { Meteor } from 'meteor/meteor';

import './methods/validate';
import './methods/genURL';
import './methods/getInstance';
import './methods/getProjection';
import './methods/shareInstance';

Meteor.startup(() => {
    // code to run on server at startup

    Todos = new Mongo.Collection('todos');
    console.log('MONGO IS ALIVE');
});
