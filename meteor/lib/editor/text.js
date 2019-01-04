/**
 * File with utils function shared by both client and server
 * with regard to text parsing
 */

export {
    isParagraph, containsValidSecret
}


/*Check if the model contains some valid 'secret'*/
function containsValidSecret(model) {
    return model.indexOf("\n//SECRET\n")!= -1


    // var i, lastSecret = 0;
    // var paragraph = "";
    // while ((i = model.indexOf("//SECRET\n", lastSecret)) >= 0) {
    //     for (var z = i + ("//SECRET\n".length);
    //         (z < model.length && model[z] != '{'); z++) {
    //         paragraph = paragraph + model[z];
    //     }
    //     if (!isParagraph(paragraph)) {
    //         paragraph = "";
    //         lastSecret = i + 1;
    //         continue;
    //     }
    //     if (findClosingBracketMatchIndex(model, z) != -1) {
    //         return true;
    //     }
    //     lastSecret = i + 1;
    // }
    // return false;
}

/**
 * Check if word is a valid paragraph
 * @param word the word to check
 * @return true if valid paragraph, false otherwise
 */
function isParagraph(word) {
    var pattern_named = /^((one sig |sig |pred |fun |abstract sig )(\ )*[A-Za-z0-9]+)/m;
    var pattern_nnamed = /^((fact|assert|run|check)(\ )*[A-Za-z0-9]*)/m;
    if (word.match(pattern_named) == null && word.match(pattern_nnamed) == null) return false;
    else return true;
}