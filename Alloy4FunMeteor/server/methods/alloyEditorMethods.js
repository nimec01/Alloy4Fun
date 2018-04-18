/**
 * Created by josep on 09/02/2016.
 */

//url="http://alloy4funvm.di.uminho.pt:8080/Alloy4Fun/services/AlloyService?wsdl";
url="http://localhost:8080/Alloy4Fun/services/AlloyService?wsdl";


/* Meteor server methods */
Meteor.methods({
/*
  Uses webservice to get a model instance
      'forceInterpretation' : used to skip cache and force new model interpretation
      'cid' : link_id  , 'derivatedOf' model
              "Original" otherwise
*/
    'getInstance' : function (model, sessionId, instanceNumber, commandLabel, forceInterpretation,cid){

      /* Normal behaviour */
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

      var resultObject = JSON.parse(result.return);

      /*Só grava stats se tiver um link associado e este for público */
      if (cid != "Original"){

        link = Link.findOne({_id:cid});
        if(!link.private){
          modelid = link.model_id;
          var sat = (result.unsat) ? false : true;
          command = getCommand(model,commandLabel);
          var storableRun = {
            sat : sat,
            model: modelid,
            command: command
          }
          Run.insert(storableRun);
        }
      }

      /*Contents of executed models must be stored in the Run collection for statistics use,
        only, the state of the model must be saved before */
        /*
        if (forceInterpretation){
          /* Command must be extracted from model plus commandLabel
          command = getCommand(model,commandLabel);

          var storableModel = {
                whole: model,
                /*Rever isto, por enquanto derivationOf: Original
                derivationOf: "Original"
          }

          var modelid= Model.insert(storableModel); /*Não é nescessário link para este modelo, pois é inacessível do exterior

          var sat = (result.unsat) ? false : true;

          /* command used must be added to the object for stats
          var storableRun = {
            sat : sat,
            model: modelid,
            command: command
          }
          console.log("Run entry: " + storableRun);
          Run.insert(storableRun)
        }*/


      if(resultObject.syntax_error){
          throw new Meteor.Error(502, resultObject);
      } else {
          resultObject.number=instanceNumber;
          return resultObject;
      }
  },

/*
  Stores the model specified in the function argument, returns model url 'id'
   used in Share Model option
*/
    'genURL' : function (model,current_id) {

        var modeldOf = "Original";

        /* if its not an original model */
        if (current_id != "Original"){
          var link = Link.findOne({_id:current_id});
          modeldOf = link.model_id;
        }

        /*A Model is always created, regardless of having secrets or not */
        var newModel_id  = Model.insert({
                            whole: model,
                            derivationOf : modeldOf
                         });


        /*A public link is always created as well*/
        var public_link_id = Link.insert({
            model_id : newModel_id,
            private: false
        });

        /*
          result : public link : case the model haven't any secret
                   (public link , private link) : case the model contains_valid_secret(s)
        */
        var result;

        /* --- Cointans_valid_secret logic ---- */
        var contains_valid_secret = false;
        var i,j,lastSecret = 0;
        var paragraph = ""
        while( (i = model.indexOf("//SECRET\n",lastSecret)) >= 0){
          for(var z = i+("//SECRET\n".length) ; (z<model.length && model[z]!='{'); z++){
              paragraph = paragraph + model[z];
          }
          var para_pattern = /^((one sig |sig |module |open |fact |pred |assert |fun |run |check |abstract )(\ )*[^ ]+)/;
          if(paragraph.match(para_pattern) == null) {paragraph = ""; lastSecret = i + 1 ; continue;}
          if( findClosingBracketMatchIndex(model, z) != -1) {contains_valid_secret = true; break;}
          lastSecret = i + 1 ;
          paragraph =
          console.log("no ciclo!");
        }
        /* ------------------------------------- */

        if (contains_valid_secret){
            var private_link_id=Link.insert({
                model_id : newModel_id,
                private: true
            });
            var result={
                public: public_link_id,
                private: private_link_id
            }
        }else{
            result={
                public: public_link_id
            }
        }

        return result;
      },

/*
  Stores model instance, returns url to make possible share the instance.
   used in Share Instance option
*/
    'storeInstance' : function (model, themeData, instance){
        /*
        var id = Instance.insert({
            model: model,
            graph: instance, /*object type non-printable
            theme: themeData /*object type non-printable
        });
        */
        return "pendente";
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

      /*
        'id' it's a private link
      */
      'getStatistics' : function(id){

        var numberOfDerivations = 0, satisfiableOutcomes = 0;
        link = Link.findOne({_id:id});
        if(link) var model_id=link.model_id;

        /* derivations */
        var derivations = Model.find({derivationOf: model_id}).fetch();
        if((dl = derivations.length) > 0) numberOfDerivations = dl;

        /*outcomes*/ /*é nescessário limitar a pesquisa a comandos que sejam comandos objetivo "challenges"*/
        var runs = Run.find({model: model_id},{sat : true}).fetch();
        if((rl = runs.length) > 0){ satisfiableOutcomes = rl;}


        var result = {numberOfDerivations : numberOfDerivations, satisfiableOutcomes : satisfiableOutcomes};
        return result;
        }

    });


/* ------- AUX methods ------- */
    function findClosingBracketMatchIndex(str, pos) {
        if (str[pos] != '{') {
            throw new Error("No '{' at index " + pos);
        }
        var depth = 1;
        for (var i = pos + 1; i < str.length; i++) {
            switch (str[i]) {
                case '{':
                    depth++;
                    break;
                case '}':
                    if (--depth == 0) {
                        return i;
                    }
                    break;
            }
        }
        return -1;    // No matching closing parenthesis
    }

  /*
    From 'model' get the command with the label specified on 'commandLabel'
  */
    function getCommand(model,commandLabel) {
      var re = new RegExp("((run |check |assert )(\ )*)" + commandLabel);
      command = model.match(re);
      return command[0]; /*returns the biggest match */

    }
