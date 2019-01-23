/**
 * Utility functions for parsing and analysis of code of Alloy models, shared
 * by both client and server.
 */

export {
    containsValidSecret,
    getCommandsFromCode,
    secretTag,
    paragraphKeywords
}

/** The secret tag used in Alloy code. */
secretTag = "//SECRET"
/** The keywords that identify paragraphs. */
paragraphKeywords = "sig|fact|assert|check|fun|pred|run"

/**
 Checks whether a the code of an Alloy model contains some valid 'secret' tag
 (i.e., a line exactly "//SECRET"). No white-spaces allowed before/after.

 @param {String} code the Alloy model with the potential secret

 @return true if there is a secrete tag 
 */
function containsValidSecret(code) {
    return (code.indexOf("\n"+secretTag+"\n") != -1 || code.indexOf(secretTag+"\n") == 0);
}

/**
 Calculates a list of identifiers for the run/check commands defined in the
 code of an Alloy model. If named, returns name, otherwise, returns indexed
 "run$"/"check$".
 
 @param {String} code the Alloy model to be analysed

 @return a list of identifiers for commands in the code
 */
function getCommandsFromCode(code) {
    let pattern = /((\W|^)run(\{|(\[\n\r\s]+\{)|([\n\r\s]+([^{\n\r\s]*)))|((\W|^)check(\{|(\[\n\r\s]+\{)|([\n\r\s]+([^{\n\r\s]*)))))/g;
    let commands = [];
    let commandNumber = 1;

    // To avoid commands that are in comment, comments must be eliminated
    // before parse
    code = code.replace(/\/\/(.*)(\n)/g, "");
    let matches = pattern.exec(code);

    while (matches != null) {
        let pre = matches[0].includes("run") ? "run " : "check "
        if (matches[6]) commands.push(pre + matches[6]);
        else if (matches[12]) commands.push(pre + matches[12]);
        else commands.push(pre + commandNumber);
        commandNumber++;
        matches = pattern.exec(code);
    }

    return commands
}