import {
    chai,
    assert
} from 'meteor/practicalmeteor:chai';
import {
    isParagraph,
    containsValidSecret,
    getCommandsFromCode,
    secretTag,
    extractSecrets
} from "./text"
/**
 * Default meteor tests for programming principles to be forced
 */
describe("editor text util functions", function() {
    it("identifies invalid secrets", function() {
        chai.assert.isFalse(containsValidSecret("/*\n//SECRET\n*/\nsig a {}"))
        chai.assert.isFalse(containsValidSecret("something"))
        chai.assert.isFalse(containsValidSecret("something/SECRET"))
        chai.assert.isFalse(containsValidSecret("something//SECRET\n"))
        chai.assert.isFalse(containsValidSecret("something\n//SECRET\nthis is the secret"))
        chai.assert.isFalse(containsValidSecret("\n//SECRET\nthis is the secret"))
        chai.assert.isFalse(containsValidSecret("sig a {} //SECRET\n"))
    });
    it("identifies valid secrets", function() {
        chai.assert.isTrue(containsValidSecret(" //SECRET\nsig a {}"))
        chai.assert.isTrue(containsValidSecret("//SECRET\nsig a {}"))
        chai.assert.isTrue(containsValidSecret("//SECRET  \nsig a {}"))
    });
    it("identifies correct commands in code", function() {
        let code = `
// run shouldNotDetect { no d: Dir | d in d.^contents }
// check shouldNotDetect2 { no d: Dir | d in d.^contents }
run run1 for 5
run run2
run run3 {}
check check1 for 5
check check2
`
        chai.assert.sameMembers(getCommandsFromCode(code), ["run run1", "run run2", "run run3", "check check1", "check check2"])
    });
});

describe("extracting secrets method", function() {
    let code, res
    it("returns empty public and secret", function() {
        code = ``
        res = extractSecrets(code)
        chai.assert.equal(res.public, "")
        chai.assert.equal(res.secret, "")
    });
    it("returns empty secret and correct public", function() {
        code = `sig A {}`
        res = extractSecrets(code)
        chai.assert.equal(res.secret, "")
        chai.assert.equal(res.public, code)

        code = `
sig A {}
pred checkStuff{

}`
        res = extractSecrets(code)
        chai.assert.equal(res.public, code)
        chai.assert.equal(res.secret, "")

        code = `
/* //SECRET */
sig A {}
pred checkStuff{

}`
        res = extractSecrets(code)
        chai.assert.equal(res.public, code)
        chai.assert.equal(res.secret, "")

        code = `
sig A {} //SECRET
pred checkStuff{

}`
        res = extractSecrets(code)
        chai.assert.equal(res.public, code)
        chai.assert.equal(res.secret, "")
    });
    it("returns empty public and correct secret", function() {
        code = `
//SECRET
sig A {}`
        res = extractSecrets(code)
        chai.assert.equal(res.public, "\n")
        chai.assert.equal(res.secret, code.substr(1))

        code = `
//SECRET
sig A {}
//SECRET
pred checkStuff{

}`
        res = extractSecrets(code)
        chai.assert.equal(res.public, "\n")
        chai.assert.equal(res.secret, code.substr(1))
    });
    it("returns correct public and secret", function() {
        let public_code = `
sig Employee{}

sig Department{}
one sig Company {
    isDirectorOf: Employee -> Department
}

//write a prediate Quizz to check that
fact Quizz {
    // In a company, each department has exactly one director (chosen among 
    // the company's employees), but each employee can only be the director 
    // of at most one department
        all d: Department | one  Company.isDirectorOf.d
    all e: Employee   | lone Company.isDirectorOf[e]
}`
        let private = `//SECRET
assert validQuizz {
        all d: Department | one  Company.isDirectorOf.d 
    all e: Employee   | lone Company.isDirectorOf[e]
}

//SECRET
check validQuizz for 5`
        code = public_code + private
        res = extractSecrets(code)
        chai.assert.equal(res.public, public_code)
        chai.assert.equal(res.secret, private)
    });
});