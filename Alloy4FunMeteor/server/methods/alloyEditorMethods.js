/**
 * Created by josep on 09/02/2016.
 */

//WSDL URL
//debugging webservice locally
//url="http://alloy4funvm.di.uminho.pt:8080/Alloy4Fun/services/AlloyService?wsdl";
url="http://localhost:8080/Alloy4Fun/services/AlloyService?wsdl";

Meteor.methods({
    'getInstance' : function (model, sessionId, instanceNumber, commandLabel, forceInterpretation){
        var args = {model: model, sessionId: sessionId, instanceNumber: instanceNumber, commandLabel: commandLabel, forceInterpretation: forceInterpretation};
        try {
            var client = Soap.createClient(url);
            var result = client.getInstance(args);
        }
        catch (err) {
            if(err.error === 'soap-creation') {
                throw new Meteor.Error(500, "We're sorry! The service is currently unavailable. Please try again later.");
            }
            else if (err.error === 'soap-method') {
                throw new Meteor.Error(501, "We're sorry! The service is currently unavailable. Please try again later.");
            }
        }
        //var resultObject = JSON.parse(result.getInstanceReturn);
        var resultObject = JSON.parse(result.return);

        //console.log("result = ");
        //console.log(result);

        if(resultObject.syntax_error){
            throw new Meteor.Error(502, resultObject);
        } else {
            resultObject.number=instanceNumber;
            return resultObject;
        }

    },
    'genURL' : function (model) {
        var id=Model.insert({
            model: model
        });
        return id;
    },
    'storeInstance' : function (model, themeData, instance){
        var id = Model.insert({
            model: model,
            instance: instance,
            themeData: themeData
        });
        return id;
    },
    'storeChallenge' : function (wholeChallenge, password, public, derivationOf, lockedLines) {
        var regexSecret = /\/\/START_SECRET(?:(?!\/\/END_SECRET)[^/])*\/\/END_SECRET/g;
        var regexCheck = /((?:\s(check|run)\s+)([a-zA-Z][a-zA-Z0-9_"'$]*)?(?:\s*\{[^}]*}))/g;
        var challenges = [];
        var secretBlock, checkCommand;
        while (secretBlock = regexSecret.exec(wholeChallenge)) {
            while(checkCommand = regexCheck.exec(secretBlock)){
                if (checkCommand && checkCommand[3])challenges.push({name: checkCommand[3], value: checkCommand[1], commandType: checkCommand[2]});
                //Error: Unnamed check command.
                else throw new Meteor.Error(503,
                    wholeChallenge.split(checkCommand[3].trim())[0].split(/\r\n|\r|\n/).length);
            }
        }
        var storableChallenge = {
            whole: wholeChallenge,
            lockedLines : lockedLines,
            challenges: challenges,
            password: password,
            public : public,
            derivationOf : derivationOf
        }

        var id = Challenge.insert(storableChallenge);
        console.log("store challenge feito")
        return id;

        throw new Meteor.Error(505, "Server error.");
    },
    'assertChallenge' : function (model, id, command, sessionId, instNumber, forceInterpretation){
        var challenge = Challenge.findOne({_id: id});
        var storedCommand = challenge.challenges.filter((i)=>{return i.name==command});
        var commandType = storedCommand.length > 0? storedCommand[0].commandType : undefined;
        var whole = challenge.whole;
        var modelWithSecret = model;
        var secretRegex = /\/\/START_SECRET(?:(?!\/\/END_SECRET)[^/])*\/\/END_SECRET/g;
        var secretBlock;
        while(secretBlock = secretRegex.exec(whole)){
            modelWithSecret= modelWithSecret.concat(secretBlock);
        }
        var args = {model: modelWithSecret, sessionId: sessionId, instanceNumber: instNumber, commandLabel: command, forceInterpretation: forceInterpretation};
        try {
            var client = Soap.createClient(url);
            var result = client.getInstance(args);
        }
        catch (err) {
            if(err.error === 'soap-creation') {
                throw new Meteor.Error(500, "We're sorry! The service is currently unavailable. Please try again later.");
            }
            else if (err.error === 'soap-method') {
                throw new Meteor.Error(501, "We're sorry! The service is currently unavailable. Please try again later.");
            }
        }

        //var resultObject = JSON.parse(result.getInstanceReturn);
        var resultObject = JSON.parse(result.return);
        if(resultObject.syntax_error){
            console.log("error no assert")
            throw new Meteor.Error(502, resultObject);
        } else {

            resultObject.number=0;
            resultObject.commandType= commandType;
            return resultObject;
        }

        return 1;

    },
    //Every time a challenge is changed and executed, store it as a derivation of the original
    'storeDerivation' : function(model, derivationOf, sat){
        var challenge = Challenge.findOne({_id: derivationOf});

        var regex_secret = /(\/@[^\/]*@\/)/g;
        var matches_secret = [];
        var match_secret;
        while (match_secret = regex_secret.exec(challenge))matches_secret.push(match_secret[1]);

        var storableChallenge = {
            whole: model + matches_secret.join("\n"),
            cut: model,
            challenges: challenge.challenges,
            password: challenge.password,
            public : false,
            derivationOf : derivationOf
        };

        var challengeId= Challenge.insert(storableChallenge);
        var storableRun = {
            sat : sat,
            model : challengeId
        };

        Run.insert(storableRun);

        return challengeId;
    },
    //Check if the password is correct
    'unlockChallenge' : function(id, password){
        var challenge = Challenge.findOne({_id: id});
        console.log("tentar desbloquear um desafio")
        if (password == challenge.password){
            console.log("desbloqueou")
            console.log(challenge)
            return challenge;
        }else throw new Meteor.Error(506, "Invalid password!");
    },
    //Share button.
    'storeSolution' : function(content, derivationOf,  immutable){
        var challenge = Challenge.findOne({_id: derivationOf});

        var regex_secret = /(\/@[^\/]*@\/)/g;
        var matches_secret = [];
        var match_secret;
        while (match_secret = regex_secret.exec(challenge))matches_secret.push(match_secret[1]);

        var storableSolution = {
            whole: content + matches_secret.join("\n"),
            immutable : immutable,
            challenges: challenge.challenges,
            password: challenge.password,
            derivationOf : derivationOf,
            public : true
        }

        var solution = Challenge.insert(storableSolution);
        return solution;
    },
    'getStatistics' : function(id){
        var queue = [];
        var numberOfDerivations = 0, satisfiableOutcomes = 0;
        queue.push(id);
        while(queue.length>0){
            var id = queue.pop();
            var derivations = Challenge.find({derivationOf : id}).fetch();
            var runs = Run.find({model : id}).fetch();
            for(var i = 0 ; i< derivations.length ; i++){
                queue.push(derivations[i]._id);
                numberOfDerivations++;
            }
            for(var i = 0 ; i< runs.length && !runs[i].sat ; i++)satisfiableOutcomes++;
        }
        var result = {numberOfDerivations : numberOfDerivations, satisfiableOutcomes : satisfiableOutcomes};
        return result;

    },
    'getProjection' : function (sessid, frameInfo){
        var args = {sessid: sessid, types : []};
        for(var key in frameInfo){
            args.types.push(key+frameInfo[key]);
        }
        try {
            var client = Soap.createClient(url);
            var result = client.getProjection(args);
        }
        catch (err) {
            if(err.error === 'soap-creation') {
                throw new Meteor.Error(500, "We're sorry! The service is currently unavailable. Please try again later.");
            }
            else if (err.error === 'soap-method') {
                throw new Meteor.Error(501, "We're sorry! The service is currently unavailable. Please try again later.");
            }
        }
        //verificar : return JSON.parse(result.return.toString());
        //return JSON.parse(result.getProjectionReturn.toString());
        return JSON.parse(result.return.toString());
    },
});
