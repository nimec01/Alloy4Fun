/**
 * Created by josep on 10/02/2016.
 */


editorLoadController = RouteController.extend({

    // A place to put your subscriptions
    // this.subscribe('items');
    // // add the subscription to the waitlist
    // this.subscribe('item', this.params._id).wait();

    template : 'alloyEditor',

    subscriptions: function () {
        this.subscribe('editorLoad', this.params._id).wait();

        //subscribe to cursor solutions. check server/publications/alloyLoadPublications
        this.subscribe('solutions', this.params._id).wait();
    },

    // Subscriptions or other things we want to "wait" on. This also
    // automatically uses the loading hook. That's the only difference between
    // this option and the subscriptions option above.
    // return Meteor.subscribe('post', this.params._id);

    waitOn: function () {
    },

    // A data function that can be used to automatically set the data context for
    // our layout. This function can also be used by hooks and plugins. For
    // example, the "dataNotFound" plugin calls this function to see if it
    // returns a null value, and if so, renders the not found template.
    // return Posts.findOne({_id: this.params._id});

    data: function () {
        var teste = this.params._id;
        var model = Model.findOne({_id: this.params._id});
        var themes = Theme.find({modelId : this.params._id}).fetch();


        //var modelCursor = Model.find();

        //var solutions = Solutions.find();
        var challengeId = Solutions.findOne({_id: this.params._id});

        if (challengeId) {



            return model;
        }

        if (model) {


            model.themes = themes;
            return model;
        }
/*
        else{ */
            var challengeId = Solutions.findOne({_id: this.params._id});
            console.log("challenge id");
            console.log(challengeId);
            var challengeToSolve = Challenge.find({_id: challengeId});

            if(challengeToSolve)
                console.log(challengeToSolve);
                return challengeToSolve;
        //}

    },

    // You can provide any of the hook options

    onRun: function () {
        this.next();
    },
    onRerun: function () {
        this.next();
    },
    onBeforeAction: function () {
        this.next();
    },

    // The same thing as providing a function as the second parameter. You can
    // also provide a string action name here which will be looked up on a Controller
    // when the route runs. More on Controllers later. Note, the action function
    // is optional. By default a route will render its template, layout and
    // regions automatically.
    // Example:
    //  action: 'myActionFunction'

    action: function () {
        this.render();
    },
    onAfterAction: function () {
    },
    onStop: function () {
    }
});