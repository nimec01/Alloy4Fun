/**
 * Utility functions for parsing and analysis of code of Alloy models, shared
 * by both client and server.
 */

export {
    containsValidSecret,
    getCommandsFromCode
}

/**
 Checks whether a the code of an Alloy model contains some valid 'secret' tag
 (i.e., a line exactly "//SECRET"). No white-spaces allowed before/after.

 @param {String} code the code with the potential secret

 @return true if there is a secrete tag 
 */
function containsValidSecret(code) {
    return (code.indexOf("\n//SECRET\n") != -1 || code.indexOf("//SECRET\n") == 0);
}

/**
 Calculates a list of identifiers for the run/check commands defined in the
 code of an Alloy model. If named, returns name, otherwise, returns indexed
 "run$"/"check$".
 
 @param {String} code the code to be analysed

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
        if (matches[6]) commands.push(matches[6]);
        else if (matches[12]) commands.push(matches[12]);
        else if (matches[0].includes("run")) {
            commands.push("run$" + commandNumber);
        } else if (matches[0].includes("check")) {
            commands.push("check$" + commandNumber);
        } else console.log("Unreachable block of code.");
        commandNumber++;
        matches = pattern.exec(code);
    }

    return commands
}