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
        this.subscribe('editorLoad'/*, this.params._id*/).wait();

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
        var link = Link.findOne({_id: this.params._id});

        var model;
        var secrets = "";
        if (link){
            model = Model.findOne({_id: link.model_id});

            //if the model is public
            if (!link.private){
                //secret is removed

                //whole is where the whole model is stored
                var v = model.whole;

                var i;

                //indexOf -> returns the position of the first occurrence of a specified value in a string.
                while ((i = v.indexOf("//SECRET"))>=0) {


                    var z = i;
                    for(z;v[z]!='{';z++);

                    var e = findClosingBracketMatchIndex(v,z)
                    //para o modelo de paragrafo pesquisar o fim do paragrafo
                    //var e = v.indexOf("//END_SECRET");

                    //substr -> extracts parts of a string, beginning at the character at the specified position,
                    //          and returns the specified number of characters. [string.substr(start, length)]

                    //SECRETS DE V para a variável "secrets"
                    secrets+="\n"+v.substr(i+1, e-i+1); //12="//END_SECRE

                    //retira secret de v
                    v = v.substr(0, i) + v.substr(e +1);
                }

                //_______________LOCK LINES______________________
                var lockedLines = [];
                //parse do whole apos tirados secrets (em v) para obter os locked paragraphs
                //lines -> array of lines
                var lines = v.split(/\r?\n/);
                var l=0;
                while (l<lines.length) {
                    var line = lines[l];
                    //trim removes white spaces
                    if (line.trim() == "//LOCK") {
                        //lockedLines.push(l+1);//line numbers in editor are '1' based
                        l++;
                        //last line is where the paragraph ends
                        var lastLine = findParagraph(lines, l);

                        if (lastLine!=-1){
                            lockedLines.push(l/*-1+1*/);//line numbers in editor are '1' based
                            while (l < lastLine + 1) {
                                lockedLines.push(l + 1);
                                l++
                            }
                        }
                    }else
                        l++;
                }


                var secretCommands = [];
                var secretsAux= secrets.split(" ");

                //deixa este to do comigo, mas não tive tempo para mais
                //TODO : Ricardo -> sacar nomes dos runs, checks e asserts. Os que não têmm nome, os que têm chaveta à frente (run A{.....), os que tem \n logo à frente (run a\n{....)
                for(z=0;z<secretsAux.length;z++)
                    if(secretsAux[z]=="run" ||
                        secretsAux[z]=="check" ||
                        secretsAux[z]=="assert")
                            secretCommands.push(secretsAux[z+1])


                model = {
                    "_id": model._id,
                    "whole": v,
                    "secrets": secrets,
                    "secretCommands" : "",
                    "lockedLines":lockedLines
                }
                //model.whole = v;
                //model.secrets=secrets
            }
            //Link.find().fetch();
            //Model.find().fetch();
        }else{
            model = {
                "_id": -1,
                "whole": "Link não encontrado",
                "secrets": ""
            }
        }

        return model;


        //[ainda vou fazer]
        var themes = Theme.find({modelId : this.params._id}).fetch();
        model.themes = themes;






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
    let depth = 1;
    for (let i = pos + 1; i < str.length; i++) {
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



