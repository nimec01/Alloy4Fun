/**
 * Created by josep on 10/02/2016.
 */

var Schema = {};

//Models created through the editor feature.
/*Schema.Model = new SimpleSchema({
    //The id can be appended to the editor's URL to load that particular model into it.
    _id: {
        type: String,
        optional: false
    },
    model: {
        type: String,
        optional: false
    },
    instance: {
        type : Object,
        optional : true
    }
});*/

//Challenges created through the challenge creation page.
Schema.Challenge = new SimpleSchema({
    _id: {
        type: String,
        optional: false
    },
    whole: {
        type: String,
        optional: false
    },
    lockedLines: {
        type : Array,
        optional : true
    },
    "lockedLines.$" : {
        type: Number
    },
    cut : {
        type: String,
        optional : false,
        autoValue : function () {
            return this.field("whole").value.replace(/\/\/START_SECRET(?:(?!\/\/END_SECRET)[^/])*\/\/END_SECRET/g, "").replace(/\/\/START_LOCK/g, "\n").replace(/\/\/END_LOCK/g, "\n");
        }
    },
    challenges : {
        type : Array,
        optional : true
    },
    "challenges.$" : {
        type : Object
    },
    "challenges.$.name" : {
        type : String
    },
    "challenges.$.value" : {
        type : String
    },
    "challenges.$.commandType" : {
        type : String
    },
    password : {
        type: String,
        optional : true
    },
    derivationOf : {
        type: String,
        optional : false
    },
    public : {
        type : Boolean,
        optional : false
    }
});

Schema.Run = new SimpleSchema({
    _id: {
        type: String,
        optional: false
    },
    sat: {
        type: Boolean,
        optional: false
    },
    model : {
        type: String,
        optional : false
    }
});

Schema.Theme = new SimpleSchema({
    _id: {
        type: String,
        optional: false
    },

    name:{
        type : String,
        optional: false
    },

    //Node Colors
    nodeColors: {
        type : Array,
        optional : true
    },
    "nodeColors.$":{
        type : Object
    },
    "nodeColors.type":{
        type : String
    },
    "nodeColors.color":{
        type: String
    },

    //Node Shapes
    nodeShapes: {
        type : Array,
        optional : true
    },
    "nodeShapes.$":{
        type : Object
    },
    "nodeShapes.type":{
        type : String
    },
    "nodeShapes.shape":{
        type: String
    },

    /*nodePositions: {
        type: Array,
        optional : true
    },
    "nodePositions.$" : {
        type: Object
    },
    "nodePositions.id" : {
        type: String,
        optional : false
    },
    "nodePositions.pos" : {
        type: Object
    },
    "nodePositions.pos.x" : {
        type : Number
    },
    "nodePositions.pos.y" : {
        type: Number
    },*/

    //Node labels in case of renaming
    nodeLabels : {
        type: Array,
        optional : true
    },
    "nodeLabels.$" : {
        type : Object
    },
    "nodeLabels.type" : {
        type : String
    },
    "nodeLabels.label" : {
        type: String
    },


    //Edge Colors
    edgeColors: {
        type : Array,
        optional : true
    },
    "edgeColors.$":{
        type : Object
    },
    "edgeColors.relation":{
        type : String
    },
    "edgeColors.color":{
        type: String
    },

    //Edge Labels
    edgeLabels: {
        type : Array,
        optional : true
    },
    "edgeLabels.$":{
        type : Object
    },
    "edgeLabels.relation":{
        type : String
    },
    "edgeLabels.label":{
        type: String
    },

    //Reference to its model
    modelId : {
        type: String,
        optional : false
    },

    //Edge Colors

});

/*Schema.Instance = new SimpleSchema({
    _id: {
        type: String,
        optional: false
    },
    run : {
        type: String,
        optional : false
    },
    graph : {
        type : Object,
        optional : false
    }
})*/

Schema.Solutions = new SimpleSchema({
   _id: {
      type : String,
      optional : false
   },
   theChallenge: {
        type : String,
        optional : false
  }
});

//Model.attachSchema(Schema.Model);
Challenge.attachSchema(Schema.Challenge);
Run.attachSchema(Schema.Run);
Theme.attachSchema(Schema.Theme);
Solutions.attachSchema(Schema.Solutions);
//Instance.attachSchema(Schema.Instance);
