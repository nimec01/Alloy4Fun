import {
    containsValidSecret,
    secretTag,
    paragraphKeywords
} from "../../lib/editor/text"

export {
    extractSecrets
}

/**
 Splits the Alloy code of a model between public and private paragraphs.
 Private paragraphs are preceeded by a secret tag.
 
 @param {String} code the complete code with possible secrets
 @return the public and private paragraphs of the code 
*/
function extractSecrets(code) {
    let secret = "",
        public_code = "";
    let s, i;
    let tag = secretTag.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    let pgs = paragraphKeywords
    let exp = `(?:\\/\\*(?:.|\\n)*?\\*\/|(${tag}\\s*?\\n\\s*(?:(?:(?:abstract|one|lone|some)\\s+)*${pgs})(?:.*|\\n)*?)(?:${tag}\\s*?\\n\\s*)?(?:(?:(?:one|abstract|lone|some)\\s+)*${pgs}|$))`
    console.log(RegExp(exp))
    while (s = code.match(RegExp(exp))) {
        if (s[0].match(/^\/\*(?:.|\n)*?\*\/$/)) {
            i = code.indexOf(s[0]);
            public_code += code.substr(0, i + s[0].length);
            code = code.substr(i + s[0].length);
        } else {
            i = code.indexOf(s[0]);
            public_code += code.substr(0, i);
            secret += s[1];
            code = code.substr(i + s[1].length);
        }
    }
    public_code += code;
    return {
        public: public_code,
        secret: secret
    };
}