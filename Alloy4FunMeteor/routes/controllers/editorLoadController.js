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
        //subscribe to curso editorLoad -> Model collection
        this.subscribe('editorLoad').wait();

        //subscribe to cursor solutions. check server/publications/alloyLoadPublications
        this.subscribe('solutions', this.params._id).wait();
    },

    // Subscriptions or other things we want to "wait" on. This also
    // automatically uses the loading hook. That's the only difference between
    // this option and the subscriptions option above.
    // return Meteor.subscribe('post', this.params._id);

    waitOn: function () {
    },

    data: function () {
        var priv = false;
        var link = Link.findOne({_id: this.params._id});
        var model;
        var secrets = "";

        /*------- SECRETs handler ---------- */
        if (link){
            model = Model.findOne({_id: link.model_id});
            var v = model.whole;
            if (!link.private){ /*if the model is public*/
                //secret is removed
                var i;

                var nsecrets = 0;
                while ((i = v.indexOf("//SECRET\n"))>=0) { /*while Contains secrets*/
                  var z = i;
                  var j = 0;
                  var word = "";
                  for(z; v[z]!= '\n';z++);
                  z++; /*goto next line*/
                  for(z;v && v[z]!='{';z++){
                      word += v[z]; j++;
                  }
                  if (!(isParagraph(word))){ break; /*retira //SECRET */} /*break case 'word' is not a paragraph */

                  try{
                    var e = findClosingBracketMatchIndex(v,z);
                  }catch(err){
                    break;
                  }

                 secrets+="\n\n"+v.substr(i,(e-i)+1) ;

                  v = v.substr(0,i) + v.substr(e+1); /* remove secrets from v (whole model) */
                }
           /*------------LOCK handler---------------*/
                var lockedLines = [];
                var lines = v.split(/\r?\n/); /*Array of lines */
                var l=0;
                var modelToEdit="";
                var numLockedLines=0;
                while (l<lines.length) {
                    var line = lines[l];
                    if (line.trim() == "//LOCKED") {
                        numLockedLines++;

                        //recheck if there are more lines
                        if (l>=lines.length)
                            break;

                        l++;
                        //last line is where the paragraph ends
                        var lastLine = findParagraph(lines, l);

                        if (lastLine!=-1){
                            //lockedLines.push(l - numLockedLines);//line numbers in editor are '1' based
                            while (l < lastLine + 1) {
                                lockedLines.push(l + 1 - numLockedLines);
                                modelToEdit+=lines[l]+"\n";
                                l++;
                            }
                        }
                    }else {
                        modelToEdit+=line+"\n"; //add new line to the last line
                        l++;
                    }
                }

                model = {
                    "whole": modelToEdit,
                    "secrets": secrets,
                    "lockedLines":lockedLines
                }
            }else { //TEST
                    model = {
                        "whole": v,
                        "priv": true
                    }
            }
        }else{
            model = {
                "whole": "Link não encontrado",
                "secrets": ""
            }
        }

        return model;

        //TODO
        /*
        var themes = Theme.find({modelId : this.params._id}).fetch();
        model.themes = themes;
        */
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

function findParagraph(lines, l) {
    //locate the start of the next paragraph {
    var foundStart=false;
    var braces=0;
    while (l<lines.length) {
        var line = lines[l];
        if (line.trim().length>0) {//empty lines or lines with white space are ignored
            for (var c = 0; c < line.length; c++) {
                if (!foundStart) {
                    if (line.charAt(c) == "{"
                        &&
                        line.substr(0, c).trim()
                             .match("^(one sig |sig |module |open |fact |pred |assert |fun |run |check )")
                        ) {
                            foundStart = true;
                            braces++;
                        }
                }
                //se encontrou
                else if (line.charAt(c) == "{")
                    braces++;
                else if (line.charAt(c) == "}")
                    braces--;

                if (foundStart && braces == 0)
                    return l;//this is the last line of this paragraph
            }
            if (!foundStart) //if found a non empty line but the token is not valid the LOCK is ignored
                return -1;
        }
        l++;
    }

    return l-1;
}

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

function isParagraph(word){
    var pattern = /^((one sig |sig |module |open |fact |pred |assert |fun |run |check |abstract)(\ )*[^ ]+)/;
    if(word.match(pattern) == null) return false ;
    else return true;

}
