/**
 * Created by josep on 10/02/2016.
 */

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

Meteor.publish('challenge', function (_id) {
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

    var result = Challenge.find(selector, options);

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