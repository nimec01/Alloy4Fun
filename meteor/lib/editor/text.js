/**
 * File with utils function shared by both client and server
 * with regard to text parsing
 */

export {
    isParagraph
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