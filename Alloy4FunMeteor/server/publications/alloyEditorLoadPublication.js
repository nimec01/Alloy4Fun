/**
 * Created by josep on 10/02/2016.
 */


//publish and subscribe method is used so that clients can only access specific data from the database
//to access is necessary to "subscribe"


Meteor.publish('editorLoad', function (_id) {

    var selector = {
        _id: _id
    };

    var options = {
        fields : {
            model: 1,
            instance : 1,
            themeData : 1
        }
    }


    var result = Model.find(selector, options);

    if (result) {
        return result;
    }
    return this.ready();
});


//in controllers>solveChallengeCOntroller.js: this.subscribe('challenge', this.params._id).wait();
//publish to solve challenge
Meteor.publish('challenge', function (_id) {

    //ID do challenge
    // Selector: {_id : XXXXXX}
    var selector = {
        _id : _id
    };

    var options = {
        fields: {
            cut: 1,
            "challenges.name" : 1,
            solution: 1,
            lockedLines: 1
        }
    };

    //puts query return in result variable and is captured in "solveChallengeController.js"
    var result = Challenge.find(selector, options);

    if (result) {
        return result;
    }
    return this.ready();
});


//publish for solutions [to be removed ahead]
Meteor.publish('solutions', function (_id) {

    //ID do challenge
    // Selector: {_id : XXXXXX}
    var selector = {
        _id : _id
    };

    var options = {
        fields: {
            theChallenge: 1,
        }
    };

    //puts query return in result variable and is captured in "editLoadController.js"
    var result = Solutions.find(selector, options);

    if (result) {
        return result;
    }
    return this.ready();
});


Meteor.publish('editChallenge', function (_id, password){
    var selector = {
        _id : _id
    };

    var options = {
        fields: {
            whole : 1,
            password : 1,
            lockedLines : 1
        }
    };

    var result = Challenge.find(selector, options);
    if(password == result.fetch()[0].password) {
        return result;
    }else throw new Meteor.Error(505, "Wrong password");
    return this.ready();
});